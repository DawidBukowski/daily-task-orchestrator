package com.dailytask.adapters.analyzers;

import java.util.List;

/**
 * Data Transfer Object representing Claude's parsed task summarization response.
 *
 * <p>This record provides an immutable structure for the JSON response from Claude,
 * with safe defaults for missing fields. It acts as a discriminated union boundary
 * between the external API contract and internal domain models.
 *
 * @param summary overall task landscape summary (never null, defaults to "No summary")
 * @param schedule recommended scheduling strategy (never null, defaults to "No schedule")
 * @param recommendations list of actionable advice (never null, defaults to empty list)
 * @param taskUpdates list of task modifications (never null, defaults to empty list)
 */
public record ClaudeTaskSummaryResponse(
        String summary,
        String schedule,
        List<String> recommendations,
        List<TaskUpdate> taskUpdates
) {

    /**
     * Represents a single task update with priority and time estimate changes.
     *
     * <p>Each update must reference an existing task ID for matching. Priority
     * values are validated against the Priority enum during parsing.
     *
     * @param taskId the exact task ID to update (must match existing task)
     * @param priority the new priority level (CRITICAL, HIGH, MEDIUM, or LOW)
     * @param estimatedHours the estimated time in hours (must be positive)
     * @param notes justification for the priority and estimate changes
     */
    public record TaskUpdate(
            String taskId,
            String priority,
            Double estimatedHours,
            String notes
    ) {}

    /**
     * Creates a fallback response when parsing fails or no valid data is available.
     *
     * <p>This method provides a safe default that won't disrupt the application
     * flow when Claude's response is malformed or unavailable.
     *
     * @return a response with default "no data" values
     */
    public static ClaudeTaskSummaryResponse createFallback() {
        return new ClaudeTaskSummaryResponse(
                "No summary available",
                "No schedule available",
                List.of(),
                List.of()
        );
    }

    /**
     * Creates a response with default values for missing fields.
     *
     * <p>This ensures that partially valid responses can still be processed
     * without throwing exceptions.
     *
     * @param summary the summary text (null-safe)
     * @param schedule the schedule text (null-safe)
     * @param recommendations the recommendations list (null-safe)
     * @param taskUpdates the task updates list (null-safe)
     * @return a response with non-null values
     */
    public static ClaudeTaskSummaryResponse createWithDefaults(
            String summary,
            String schedule,
            List<String> recommendations,
            List<TaskUpdate> taskUpdates
    ) {
        return new ClaudeTaskSummaryResponse(
                summary != null && !summary.isBlank() ? summary : "No summary",
                schedule != null && !schedule.isBlank() ? schedule : "No schedule",
                recommendations != null ? List.copyOf(recommendations) : List.of(),
                taskUpdates != null ? List.copyOf(taskUpdates) : List.of()
        );
    }
}
