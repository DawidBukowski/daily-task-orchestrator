package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds prompts for Claude API to analyze and summarize tasks.
 *
 * <p>This builder enforces a strict JSON schema output from Claude, ensuring
 * type-safe parsing and validation of task updates, priority assignments,
 * and time estimates.
 */
public class TaskSummarizationPromptBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)");

    /**
     * Builds the system prompt that defines Claude's role and output requirements.
     *
     * <p>The system prompt establishes:
     * <ul>
     *   <li>Claude's role as a task prioritization expert</li>
     *   <li>Strict JSON schema for output</li>
     *   <li>Field-level validation requirements</li>
     *   <li>Priority enum constraints</li>
     * </ul>
     *
     * @return the system prompt defining Claude's behavior and output schema
     */
    public String buildSystemPrompt() {
        return """
            You are a task prioritization and scheduling expert. Your role is to analyze a list of tasks
            and provide actionable insights to help the user manage their workload effectively.

            You MUST respond with valid JSON matching this exact schema:

            {
              "summary": "string - brief overview of the overall task landscape (2-3 sentences)",
              "schedule": "string - recommended daily/weekly schedule strategy",
              "recommendations": ["array of strings - specific actionable advice"],
              "taskUpdates": [
                {
                  "taskId": "string - EXACT task ID from input (critical for matching)",
                  "priority": "CRITICAL|HIGH|MEDIUM|LOW - one of these exact values",
                  "estimatedHours": 3.5,
                  "notes": "string - justification for priority and estimate"
                }
              ]
            }

            CRITICAL RULES:
            1. taskId MUST exactly match an ID from the input tasks
            2. priority MUST be one of: CRITICAL, HIGH, MEDIUM, LOW (uppercase)
            3. estimatedHours MUST be a positive number (can be decimal like 2.5)
            4. All fields are required - no null values
            5. If no updates needed, taskUpdates can be empty array []
            6. Provide recommendations even if no task updates are needed

            Focus on:
            - Realistic time estimates based on task complexity
            - Priority based on deadlines and dependencies
            - Practical scheduling that respects human work capacity
            """;
    }

    /**
     * Builds the user prompt containing task data and current date context.
     *
     * <p>The user prompt includes:
     * <ul>
     *   <li>Current date for deadline calculations</li>
     *   <li>Serialized task list with all relevant fields</li>
     *   <li>Request for analysis in the specified JSON format</li>
     * </ul>
     *
     * @param tasks the list of tasks to analyze (must not be null)
     * @param today the current date for context (must not be null)
     * @return the user prompt with task data and analysis request
     * @throws IllegalArgumentException if tasks or today is null
     */
    public String buildUserPrompt(List<Task> tasks, LocalDate today) {
        if (tasks == null) {
            throw new IllegalArgumentException("tasks must not be null");
        }
        if (today == null) {
            throw new IllegalArgumentException("today must not be null");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Today's date: ").append(today.format(DATE_FORMATTER)).append("\n\n");
        prompt.append("Tasks to analyze:\n\n");

        if (tasks.isEmpty()) {
            prompt.append("(No tasks provided)\n");
        } else {
            for (Task task : tasks) {
                prompt.append("---\n");
                prompt.append("Task ID: ").append(task.getId()).append("\n");
                prompt.append("Title: ").append(task.getTitle()).append("\n");

                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    prompt.append("Description: ").append(task.getDescription()).append("\n");
                }

                if (task.getDeadline() != null) {
                    prompt.append("Deadline: ").append(task.getDeadline()).append("\n");
                }

                prompt.append("Current Priority: ").append(task.getPriority()).append("\n");

                if (task.getEstimatedHours() != null) {
                    prompt.append("Current Estimated Hours: ").append(task.getEstimatedHours()).append("\n");
                }

                if (task.getNotes() != null && !task.getNotes().isBlank()) {
                    prompt.append("Notes: ").append(task.getNotes()).append("\n");
                }

                if (task.getTags() != null && !task.getTags().isEmpty()) {
                    prompt.append("Tags: ").append(String.join(", ", task.getTags())).append("\n");
                }

                prompt.append("\n");
            }
        }

        prompt.append("\nPlease analyze these tasks and provide your response in the JSON format specified in the system prompt.\n");
        prompt.append("Focus on practical prioritization and realistic time estimates.\n");

        return prompt.toString();
    }
}
