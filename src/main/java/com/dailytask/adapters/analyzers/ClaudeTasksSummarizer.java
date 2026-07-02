package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.ports.ClaudeApiClient;
import com.dailytask.core.ports.TaskSummarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task summarizer implementation using Claude AI.
 *
 * <p>This summarizer orchestrates the full task analysis pipeline:
 * <ol>
 *   <li>Build structured prompts with task data</li>
 *   <li>Send prompts to Claude API</li>
 *   <li>Parse and validate JSON response</li>
 *   <li>Apply task updates to existing tasks</li>
 *   <li>Return enriched summary with updated tasks</li>
 * </ol>
 *
 * <p>Implements graceful degradation: any failure at any stage returns
 * a fallback summary with original tasks unchanged. This prevents cascading
 * failures and ensures the application remains functional.
 */
public class ClaudeTasksSummarizer implements TaskSummarizer {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeTasksSummarizer.class);

    private final ClaudeApiClient apiClient;
    private final TaskSummarizationPromptBuilder promptBuilder;
    private final ClaudeResponseParser responseParser;

    /**
     * Creates a new summarizer with required dependencies.
     *
     * @param apiClient the Claude API client for sending requests
     * @param promptBuilder the prompt builder for formatting task data
     * @param responseParser the parser for validating Claude's responses
     * @throws IllegalArgumentException if any parameter is null
     */
    public ClaudeTasksSummarizer(
            ClaudeApiClient apiClient,
            TaskSummarizationPromptBuilder promptBuilder,
            ClaudeResponseParser responseParser
    ) {
        if (apiClient == null) {
            throw new IllegalArgumentException("apiClient must not be null");
        }
        if (promptBuilder == null) {
            throw new IllegalArgumentException("promptBuilder must not be null");
        }
        if (responseParser == null) {
            throw new IllegalArgumentException("responseParser must not be null");
        }

        this.apiClient = apiClient;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    /**
     * Analyzes tasks using Claude AI and returns an enriched summary.
     *
     * <p>This method implements the following workflow:
     * <ol>
     *   <li>Build system and user prompts</li>
     *   <li>Send to Claude API</li>
     *   <li>Parse JSON response</li>
     *   <li>Match and apply task updates by ID</li>
     *   <li>Return summary with updated tasks</li>
     * </ol>
     *
     * <p>On any failure (API error, parsing error, etc.), returns a fallback
     * summary with original tasks unchanged. This ensures the application
     * continues functioning even when AI analysis is unavailable.
     *
     * @param tasks the list of tasks to analyze (must not be null)
     * @return a summary with AI insights and potentially updated tasks
     * @throws IllegalArgumentException if tasks is null
     */
    @Override
    public TasksSummary summarize(List<Task> tasks) {
        if (tasks == null) {
            throw new IllegalArgumentException("tasks must not be null");
        }

        logger.info("Analyzing {} tasks using Claude AI...", tasks.size());

        try {
            // Step 1: Build prompts
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(tasks, LocalDate.now());

            logger.debug("Built prompts - system: {} chars, user: {} chars",
                    systemPrompt.length(), userPrompt.length());

            // Step 2: Send to Claude API
            String rawResponse = apiClient.sendMessage(systemPrompt, userPrompt);

            if (rawResponse == null || rawResponse.isBlank()) {
                logger.warn("Received empty response from Claude API");
                return createFallbackSummary(tasks);
            }

            logger.debug("Received response from Claude: {} chars", rawResponse.length());

            // Step 3: Parse response
            ClaudeTaskSummaryResponse response = responseParser.parse(rawResponse);

            // Step 4: Apply task updates
            List<Task> updatedTasks = applyTaskUpdates(tasks, response.taskUpdates());

            logger.info("Successfully analyzed tasks: {} recommendations, {} task updates applied",
                    response.recommendations().size(),
                    response.taskUpdates().size());

            // Step 5: Return enriched summary
            return new TasksSummary(
                    updatedTasks,
                    response.summary(),
                    response.schedule(),
                    response.recommendations()
            );

        } catch (ClaudeApiClient.ClaudeApiException e) {
            logger.error("Claude API call failed: {}", e.getMessage());
            return createFallbackSummary(tasks);
        } catch (Exception e) {
            logger.error("Unexpected error during task summarization: {}", e.getMessage(), e);
            return createFallbackSummary(tasks);
        }
    }

    /**
     * Applies task updates from Claude's response to existing tasks.
     *
     * <p>This method:
     * <ul>
     *   <li>Matches updates to tasks by exact ID</li>
     *   <li>Creates new Task instances with updated fields</li>
     *   <li>Preserves tasks that have no updates</li>
     *   <li>Logs warnings for updates with no matching task</li>
     * </ul>
     *
     * @param originalTasks the original task list
     * @param updates the list of updates from Claude
     * @return a new list with updated tasks (never null)
     */
    private List<Task> applyTaskUpdates(
            List<Task> originalTasks,
            List<ClaudeTaskSummaryResponse.TaskUpdate> updates
    ) {
        if (updates == null || updates.isEmpty()) {
            logger.debug("No task updates to apply");
            return new ArrayList<>(originalTasks);
        }

        // Build update map for efficient lookup
        Map<String, ClaudeTaskSummaryResponse.TaskUpdate> updateMap = new HashMap<>();
        for (ClaudeTaskSummaryResponse.TaskUpdate update : updates) {
            updateMap.put(update.taskId(), update);
        }

        // Apply updates to matching tasks
        List<Task> updatedTasks = new ArrayList<>();
        int appliedCount = 0;

        for (Task task : originalTasks) {
            ClaudeTaskSummaryResponse.TaskUpdate update = updateMap.get(task.getId());

            if (update != null) {
                Task updatedTask = applyUpdateToTask(task, update);
                updatedTasks.add(updatedTask);
                updateMap.remove(task.getId()); // Mark as processed
                appliedCount++;
            } else {
                updatedTasks.add(task);
            }
        }

        // Log warnings for unmatched updates
        for (String unmatchedId : updateMap.keySet()) {
            logger.warn("Task update for ID '{}' has no matching task, skipping", unmatchedId);
        }

        logger.debug("Applied {} task updates out of {} requested", appliedCount, updates.size());

        return updatedTasks;
    }

    /**
     * Applies a single update to a task, creating a new Task instance.
     *
     * <p>This method preserves immutability by creating a new Task with
     * updated fields rather than mutating the original.
     *
     * @param task the original task
     * @param update the update to apply
     * @return a new Task with updated priority, estimatedHours, and notes
     */
    private Task applyUpdateToTask(Task task, ClaudeTaskSummaryResponse.TaskUpdate update) {
        // Parse priority with fallback
        Priority newPriority = Priority.fromString(update.priority());

        // Merge notes: append Claude's notes to existing notes
        String mergedNotes = mergeNotes(task.getNotes(), update.notes());

        // Create updated task
        Task updatedTask = new Task(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                newPriority,
                task.getSource(),
                task.getOriginalId(),
                task.getStatus(),
                update.estimatedHours() != null ? update.estimatedHours() : task.getEstimatedHours(),
                task.getTags(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                mergedNotes
        );

        logger.debug("Updated task {}: priority={}, estimatedHours={}",
                task.getId(), newPriority, update.estimatedHours());

        return updatedTask;
    }

    /**
     * Merges existing task notes with Claude's update notes.
     *
     * <p>Strategy:
     * <ul>
     *   <li>If both exist, append Claude's notes with a separator</li>
     *   <li>If only one exists, use that one</li>
     *   <li>If neither exists, return empty string</li>
     * </ul>
     *
     * @param existingNotes the original task notes (may be null)
     * @param updateNotes Claude's new notes (may be null)
     * @return merged notes string
     */
    private String mergeNotes(String existingNotes, String updateNotes) {
        boolean hasExisting = existingNotes != null && !existingNotes.isBlank();
        boolean hasUpdate = updateNotes != null && !updateNotes.isBlank();

        if (hasExisting && hasUpdate) {
            return existingNotes + "\n\n[AI Analysis] " + updateNotes;
        } else if (hasExisting) {
            return existingNotes;
        } else if (hasUpdate) {
            return "[AI Analysis] " + updateNotes;
        } else {
            return "";
        }
    }

    /**
     * Creates a fallback summary when analysis fails.
     *
     * <p>The fallback provides basic information without AI insights,
     * ensuring the application remains functional.
     *
     * @param tasks the original task list
     * @return a minimal summary with original tasks unchanged
     */
    private TasksSummary createFallbackSummary(List<Task> tasks) {
        return new TasksSummary(
                new ArrayList<>(tasks),
                "Task analysis unavailable. Showing " + tasks.size() + " tasks.",
                "Claude AI analysis temporarily unavailable.",
                List.of("Please try again later or check API connectivity.")
        );
    }
}
