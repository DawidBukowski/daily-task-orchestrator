package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskSummarizationPromptBuilder.
 *
 * Tests verify:
 * - System prompt contains required schema and constraints
 * - User prompt includes all task data and current date
 * - Null safety and edge case handling
 * - Prompt formatting and structure
 */
class TaskSummarizationPromptBuilderTest {

    private TaskSummarizationPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new TaskSummarizationPromptBuilder();
    }

    @Test
    void buildSystemPrompt_shouldContainRoleDefinition() {
        String systemPrompt = builder.buildSystemPrompt();

        assertNotNull(systemPrompt);
        assertFalse(systemPrompt.isBlank());
        assertTrue(systemPrompt.contains("task prioritization"));
        assertTrue(systemPrompt.contains("expert"));
    }

    @Test
    void buildSystemPrompt_shouldDefineJsonSchema() {
        String systemPrompt = builder.buildSystemPrompt();

        // Verify schema structure
        assertTrue(systemPrompt.contains("\"summary\""));
        assertTrue(systemPrompt.contains("\"schedule\""));
        assertTrue(systemPrompt.contains("\"recommendations\""));
        assertTrue(systemPrompt.contains("\"taskUpdates\""));
        assertTrue(systemPrompt.contains("\"taskId\""));
        assertTrue(systemPrompt.contains("\"priority\""));
        assertTrue(systemPrompt.contains("\"estimatedHours\""));
        assertTrue(systemPrompt.contains("\"notes\""));
    }

    @Test
    void buildSystemPrompt_shouldSpecifyPriorityConstraints() {
        String systemPrompt = builder.buildSystemPrompt();

        // Verify priority enum values
        assertTrue(systemPrompt.contains("CRITICAL"));
        assertTrue(systemPrompt.contains("HIGH"));
        assertTrue(systemPrompt.contains("MEDIUM"));
        assertTrue(systemPrompt.contains("LOW"));
    }

    @Test
    void buildSystemPrompt_shouldEmphasizeTaskIdMatching() {
        String systemPrompt = builder.buildSystemPrompt();

        assertTrue(systemPrompt.toLowerCase().contains("exact"));
        assertTrue(systemPrompt.toLowerCase().contains("taskid"));
    }

    @Test
    void buildUserPrompt_withEmptyList_shouldHandleGracefully() {
        LocalDate today = LocalDate.of(2026, 6, 30);
        List<Task> emptyTasks = List.of();

        String userPrompt = builder.buildUserPrompt(emptyTasks, today);

        assertNotNull(userPrompt);
        assertTrue(userPrompt.contains("2026-06-30"));
        assertTrue(userPrompt.contains("No tasks provided") || userPrompt.contains("Tasks to analyze"));
    }

    @Test
    void buildUserPrompt_withNullTasks_shouldThrowException() {
        LocalDate today = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () ->
                builder.buildUserPrompt(null, today)
        );
    }

    @Test
    void buildUserPrompt_withNullDate_shouldThrowException() {
        List<Task> tasks = List.of();

        assertThrows(IllegalArgumentException.class, () ->
                builder.buildUserPrompt(tasks, null)
        );
    }

    @Test
    void buildUserPrompt_shouldIncludeCurrentDate() {
        LocalDate specificDate = LocalDate.of(2026, 6, 30);
        List<Task> tasks = List.of();

        String userPrompt = builder.buildUserPrompt(tasks, specificDate);

        assertTrue(userPrompt.contains("2026-06-30"));
        // Day of week should be present (format depends on system locale)
        // Just verify the format includes parentheses for day name
        assertTrue(userPrompt.contains("(") && userPrompt.contains(")"));
    }

    @Test
    void buildUserPrompt_withSingleTask_shouldIncludeAllFields() {
        LocalDate today = LocalDate.now();

        Task task = new Task(
                "task-123",
                "Implement feature X",
                "Detailed description here",
                LocalDateTime.now().plusDays(2),
                Priority.HIGH,
                "gmail",
                "orig-456",
                TaskStatus.PENDING,
                3.5,
                List.of("urgent", "backend"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Some notes"
        );

        String userPrompt = builder.buildUserPrompt(List.of(task), today);

        // Verify all important fields are included
        assertTrue(userPrompt.contains("task-123"));
        assertTrue(userPrompt.contains("Implement feature X"));
        assertTrue(userPrompt.contains("Detailed description here"));
        assertTrue(userPrompt.contains("HIGH"));
        assertTrue(userPrompt.contains("3.5"));
        assertTrue(userPrompt.contains("Some notes"));
        assertTrue(userPrompt.contains("urgent"));
        assertTrue(userPrompt.contains("backend"));
    }

    @Test
    void buildUserPrompt_withMultipleTasks_shouldIncludeAllTasks() {
        LocalDate today = LocalDate.now();

        Task task1 = new Task(
                "task-1",
                "First task",
                null,
                null,
                Priority.MEDIUM,
                "gmail",
                null,
                TaskStatus.PENDING,
                null,
                new ArrayList<>(),
                LocalDateTime.now(),
                null,
                null
        );

        Task task2 = new Task(
                "task-2",
                "Second task",
                "Description 2",
                LocalDateTime.now().plusDays(1),
                Priority.CRITICAL,
                "calendar",
                null,
                TaskStatus.IN_PROGRESS,
                5.0,
                List.of("important"),
                LocalDateTime.now(),
                null,
                "Critical notes"
        );

        String userPrompt = builder.buildUserPrompt(List.of(task1, task2), today);

        assertTrue(userPrompt.contains("task-1"));
        assertTrue(userPrompt.contains("First task"));
        assertTrue(userPrompt.contains("task-2"));
        assertTrue(userPrompt.contains("Second task"));
        assertTrue(userPrompt.contains("MEDIUM"));
        assertTrue(userPrompt.contains("CRITICAL"));
    }

    @Test
    void buildUserPrompt_shouldHandleNullOptionalFields() {
        LocalDate today = LocalDate.now();

        Task minimalTask = new Task(
                "task-minimal",
                "Minimal task",
                null, // no description
                null, // no deadline
                Priority.LOW,
                "test",
                null,
                TaskStatus.PENDING,
                null, // no estimated hours
                null, // no tags
                LocalDateTime.now(),
                null,
                null // no notes
        );

        String userPrompt = builder.buildUserPrompt(List.of(minimalTask), today);

        // Should not crash and should include mandatory fields
        assertTrue(userPrompt.contains("task-minimal"));
        assertTrue(userPrompt.contains("Minimal task"));
        assertTrue(userPrompt.contains("LOW"));
    }

    @Test
    void buildUserPrompt_shouldRequestJsonFormat() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = List.of();

        String userPrompt = builder.buildUserPrompt(tasks, today);

        assertTrue(userPrompt.toLowerCase().contains("json"));
    }

    @Test
    void buildUserPrompt_shouldSeparateTasksWithDelimiters() {
        LocalDate today = LocalDate.now();

        Task task1 = new Task(
                "task-1", "Task 1", null, null, Priority.MEDIUM,
                "source", null, TaskStatus.PENDING, null, null,
                LocalDateTime.now(), null, null
        );

        Task task2 = new Task(
                "task-2", "Task 2", null, null, Priority.HIGH,
                "source", null, TaskStatus.PENDING, null, null,
                LocalDateTime.now(), null, null
        );

        String userPrompt = builder.buildUserPrompt(List.of(task1, task2), today);

        // Should have visual separators between tasks
        assertTrue(userPrompt.contains("---") || userPrompt.contains("Task ID:"));
    }

    // ============ Enhanced Schema Validation Tests ============

    @Test
    void buildSystemPrompt_shouldDocumentEstimatedHoursAsNumber() {
        String systemPrompt = builder.buildSystemPrompt();

        // Verify estimatedHours is shown as a number (not string)
        assertTrue(systemPrompt.contains("estimatedHours") &&
                (systemPrompt.contains("3.5") || systemPrompt.contains("number")));
    }

    @Test
    void buildSystemPrompt_shouldIncludeAllRequiredRules() {
        String systemPrompt = builder.buildSystemPrompt();

        // Verify critical rules are documented
        assertTrue(systemPrompt.contains("taskId MUST exactly match"));
        assertTrue(systemPrompt.contains("priority MUST be"));
        assertTrue(systemPrompt.contains("estimatedHours MUST be a positive number"));
    }

    @Test
    void buildSystemPrompt_shouldAllowEmptyTaskUpdatesArray() {
        String systemPrompt = builder.buildSystemPrompt();

        // Verify empty array is acceptable
        assertTrue(systemPrompt.contains("[]"));
    }

    // ============ Enhanced User Prompt Tests ============

    @Test
    void buildUserPrompt_shouldIncludeAllTaskFields() {
        LocalDate today = LocalDate.of(2026, 6, 30);

        Task completeTask = new Task(
                "task-complete",
                "Complete task",
                "Full description with details",
                LocalDateTime.of(2026, 7, 10, 15, 30),
                Priority.CRITICAL,
                "gmail",
                "orig-id-123",
                TaskStatus.IN_PROGRESS,
                8.5,
                List.of("urgent", "backend", "database"),
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 29, 14, 30),
                "Already in progress, approaching deadline"
        );

        String userPrompt = builder.buildUserPrompt(List.of(completeTask), today);

        // Verify all fields are present
        assertTrue(userPrompt.contains("task-complete"), "Task ID must be included");
        assertTrue(userPrompt.contains("Complete task"), "Title must be included");
        assertTrue(userPrompt.contains("Full description"), "Description must be included");
        assertTrue(userPrompt.contains("CRITICAL"), "Priority must be included");
        assertTrue(userPrompt.contains("8.5"), "Estimated hours must be included");
        assertTrue(userPrompt.contains("urgent"), "Tags must be included");
        assertTrue(userPrompt.contains("Already in progress"), "Notes must be included");
    }

    @Test
    void buildUserPrompt_withTaskWithoutDescription_shouldNotIncludeDescriptionSection() {
        LocalDate today = LocalDate.now();

        Task taskNoDesc = new Task(
                "task-1", "Title only", null, null, Priority.MEDIUM,
                "source", null, TaskStatus.PENDING, null, null,
                LocalDateTime.now(), null, null
        );

        String userPrompt = builder.buildUserPrompt(List.of(taskNoDesc), today);

        assertTrue(userPrompt.contains("Title only"));
        // Should not have "Description: null" or empty description line
        assertFalse(userPrompt.contains("Description:") && userPrompt.contains("null"));
    }

    @Test
    void buildUserPrompt_withTaskWithEmptyDescription_shouldNotIncludeDescriptionSection() {
        LocalDate today = LocalDate.now();

        Task taskEmptyDesc = new Task(
                "task-1", "Title", "", null, Priority.MEDIUM,
                "source", null, TaskStatus.PENDING, null, null,
                LocalDateTime.now(), null, null
        );

        String userPrompt = builder.buildUserPrompt(List.of(taskEmptyDesc), today);

        // Empty string description should not appear
        int descriptionPos = userPrompt.indexOf("Description:");
        if (descriptionPos > -1) {
            String afterDescription = userPrompt.substring(descriptionPos + 12, Math.min(descriptionPos + 20, userPrompt.length()));
            assertFalse(afterDescription.trim().isEmpty());
        }
    }

    @Test
    void buildUserPrompt_withTaskWithoutDeadline_shouldNotIncludeDeadlineSection() {
        LocalDate today = LocalDate.now();

        Task taskNoDeadline = new Task(
                "task-1", "Task", null, null, Priority.LOW,
                "source", null, TaskStatus.PENDING, null, null,
                LocalDateTime.now(), null, null
        );

        String userPrompt = builder.buildUserPrompt(List.of(taskNoDeadline), today);

        // Should not show "Deadline: null"
        assertFalse(userPrompt.contains("Deadline: null"));
    }

    @Test
    void buildUserPrompt_withTaskWithoutTags_shouldNotIncludeTagsSection() {
        LocalDate today = LocalDate.now();

        Task taskNoTags = new Task(
                "task-1", "Task", null, null, Priority.MEDIUM,
                "source", null, TaskStatus.PENDING, null, new ArrayList<>(),
                LocalDateTime.now(), null, null
        );

        String userPrompt = builder.buildUserPrompt(List.of(taskNoTags), today);

        // Should not show "Tags: []" or "Tags:" line
        int tagsPos = userPrompt.indexOf("Tags:");
        // Either no tags line, or it should have content
        if (tagsPos > -1) {
            String afterTags = userPrompt.substring(tagsPos + 5).split("\n")[0].trim();
            assertTrue(afterTags.isEmpty() || !afterTags.contains("null"));
        }
    }

    @Test
    void buildUserPrompt_withMultipleTagsPerTask_shouldIncludeAllTags() {
        LocalDate today = LocalDate.now();

        Task taskWithTags = new Task(
                "task-1", "Task", null, null, Priority.HIGH,
                "source", null, TaskStatus.PENDING, null, new ArrayList<>(),
                LocalDateTime.now(), null, null
        );
        taskWithTags.setTags(List.of("backend", "urgent", "database", "optimization"));

        String userPrompt = builder.buildUserPrompt(List.of(taskWithTags), today);

        assertTrue(userPrompt.contains("backend"));
        assertTrue(userPrompt.contains("urgent"));
        assertTrue(userPrompt.contains("database"));
        assertTrue(userPrompt.contains("optimization"));
    }

    @Test
    void buildUserPrompt_currentDateFormat_shouldIncludeDayOfWeek() {
        LocalDate specificDate = LocalDate.of(2026, 6, 30); // Monday
        List<Task> tasks = List.of();

        String userPrompt = builder.buildUserPrompt(tasks, specificDate);

        // Should include date in format "yyyy-MM-dd (Day)"
        assertTrue(userPrompt.contains("2026-06-30"));
        // Verify day name is present (in parentheses)
        assertTrue(userPrompt.matches("(?s).*2026-06-30\\s*\\([A-Za-z]+\\).*"));
    }

    @Test
    void buildUserPrompt_shouldRequestPracticalPrioritization() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = List.of();

        String userPrompt = builder.buildUserPrompt(tasks, today);

        assertTrue(userPrompt.toLowerCase().contains("practical") ||
                   userPrompt.toLowerCase().contains("prioritization"));
    }
}
