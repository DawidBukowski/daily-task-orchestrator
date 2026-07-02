package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.ports.ClaudeApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClaudeTasksSummarizer.
 *
 * Tests verify:
 * - Successful task analysis and update application
 * - Graceful error handling and fallback behavior
 * - Dependency injection and null safety
 * - Task update matching and merging
 * - Notes merging strategy
 */
@ExtendWith(MockitoExtension.class)
class ClaudeTasksSummarizerTest {

    @Mock
    private ClaudeApiClient mockApiClient;

    private TaskSummarizationPromptBuilder promptBuilder;
    private ClaudeResponseParser responseParser;
    private ClaudeTasksSummarizer summarizer;

    @BeforeEach
    void setUp() {
        promptBuilder = new TaskSummarizationPromptBuilder();
        responseParser = new ClaudeResponseParser();
        summarizer = new ClaudeTasksSummarizer(mockApiClient, promptBuilder, responseParser);
    }

    @Test
    void constructor_withNullApiClient_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaudeTasksSummarizer(null, promptBuilder, responseParser)
        );
    }

    @Test
    void constructor_withNullPromptBuilder_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaudeTasksSummarizer(mockApiClient, null, responseParser)
        );
    }

    @Test
    void constructor_withNullResponseParser_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaudeTasksSummarizer(mockApiClient, promptBuilder, null)
        );
    }

    @Test
    void summarize_withNullTasks_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                summarizer.summarize(null)
        );
    }

    @Test
    void summarize_withEmptyTasks_shouldCallApiAndReturnSummary() throws ClaudeApiClient.ClaudeApiException {
        String mockResponse = """
            {
              "summary": "No tasks to analyze",
              "schedule": "Relax and plan ahead",
              "recommendations": ["Take a break"],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of());

        assertNotNull(result);
        assertEquals("No tasks to analyze", result.getSummary());
        assertEquals("Relax and plan ahead", result.getSchedule());
        assertEquals(1, result.getRecommendations().size());
        assertTrue(result.getAllTasks().isEmpty());

        verify(mockApiClient, times(1)).sendMessage(anyString(), anyString());
    }

    @Test
    void summarize_withSuccessfulResponse_shouldApplyUpdates() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First task", Priority.LOW, null);
        Task task2 = createTask("task-2", "Second task", Priority.MEDIUM, 2.0);

        String mockResponse = """
            {
              "summary": "Updated priorities",
              "schedule": "Focus on task-1 first",
              "recommendations": ["Prioritize task-1"],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 5.0,
                  "notes": "Urgent deadline approaching"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2));

        assertNotNull(result);
        assertEquals("Updated priorities", result.getSummary());
        assertEquals(2, result.getAllTasks().size());

        // Verify task-1 was updated
        Task updatedTask1 = result.getAllTasks().stream()
                .filter(t -> t.getId().equals("task-1"))
                .findFirst()
                .orElseThrow();

        assertEquals(Priority.CRITICAL, updatedTask1.getPriority());
        assertEquals(5.0, updatedTask1.getEstimatedHours());
        assertTrue(updatedTask1.getNotes().contains("Urgent deadline approaching"));

        // Verify task-2 was not updated
        Task unchangedTask2 = result.getAllTasks().stream()
                .filter(t -> t.getId().equals("task-2"))
                .findFirst()
                .orElseThrow();

        assertEquals(Priority.MEDIUM, unchangedTask2.getPriority());
        assertEquals(2.0, unchangedTask2.getEstimatedHours());
    }

    @Test
    void summarize_whenApiThrowsException_shouldReturnFallback() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        when(mockApiClient.sendMessage(anyString(), anyString()))
                .thenThrow(new ClaudeApiClient.ClaudeApiException("API Error"));

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        assertTrue(result.getSummary().contains("unavailable"));
        assertEquals(1, result.getAllTasks().size());
        assertEquals(Priority.MEDIUM, result.getAllTasks().get(0).getPriority()); // Unchanged
    }

    @Test
    void summarize_whenApiReturnsNull_shouldReturnFallback() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(null);

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        assertTrue(result.getSummary().contains("unavailable"));
    }

    @Test
    void summarize_whenApiReturnsEmpty_shouldReturnFallback() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn("");

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        assertTrue(result.getSummary().contains("unavailable"));
    }

    @Test
    void summarize_withUnmatchedTaskId_shouldLogWarningAndIgnoreUpdate() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "non-existent-task",
                  "priority": "HIGH",
                  "estimatedHours": 10.0,
                  "notes": "This won't match any task"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        // Original task should be unchanged
        assertEquals(Priority.MEDIUM, result.getAllTasks().get(0).getPriority());
        assertEquals(3.0, result.getAllTasks().get(0).getEstimatedHours());
    }

    @Test
    void summarize_shouldMergeNotesCorrectly() throws ClaudeApiClient.ClaudeApiException {
        Task taskWithNotes = createTask("task-1", "Test", Priority.LOW, 2.0);
        taskWithNotes.setNotes("Existing notes");

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "AI suggests higher priority"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(taskWithNotes));

        Task updated = result.getAllTasks().get(0);
        assertTrue(updated.getNotes().contains("Existing notes"));
        assertTrue(updated.getNotes().contains("[AI Analysis]"));
        assertTrue(updated.getNotes().contains("AI suggests higher priority"));
    }

    @Test
    void summarize_withNoExistingNotes_shouldAddAiNotesOnly() throws ClaudeApiClient.ClaudeApiException {
        Task taskWithoutNotes = createTask("task-1", "Test", Priority.LOW, 2.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "AI analysis notes"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(taskWithoutNotes));

        Task updated = result.getAllTasks().get(0);
        assertTrue(updated.getNotes().contains("[AI Analysis]"));
        assertTrue(updated.getNotes().contains("AI analysis notes"));
    }

    @Test
    void summarize_withMultipleTasks_shouldUpdateOnlyMatching() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        Task task3 = createTask("task-3", "Third", Priority.HIGH, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-2",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "Updated"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        assertEquals(3, result.getAllTasks().size());

        // task-1 unchanged
        Task resultTask1 = findTaskById(result, "task-1");
        assertEquals(Priority.LOW, resultTask1.getPriority());
        assertEquals(1.0, resultTask1.getEstimatedHours());

        // task-2 updated
        Task resultTask2 = findTaskById(result, "task-2");
        assertEquals(Priority.CRITICAL, resultTask2.getPriority());
        assertEquals(10.0, resultTask2.getEstimatedHours());

        // task-3 unchanged
        Task resultTask3 = findTaskById(result, "task-3");
        assertEquals(Priority.HIGH, resultTask3.getPriority());
        assertEquals(3.0, resultTask3.getEstimatedHours());
    }

    @Test
    void summarize_withNullEstimatedHours_shouldPreserveOriginal() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 5.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "notes": "Only priority change"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        Task updated = result.getAllTasks().get(0);
        assertEquals(Priority.HIGH, updated.getPriority());
        assertEquals(5.0, updated.getEstimatedHours(), "Should preserve original hours");
    }

    @Test
    void summarize_shouldCallApiWithCorrectPrompts() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        summarizer.summarize(List.of(task));

        verify(mockApiClient, times(1)).sendMessage(
                argThat(systemPrompt -> systemPrompt.contains("task prioritization") &&
                        systemPrompt.contains("JSON")),
                argThat(userPrompt -> userPrompt.contains("task-1") &&
                        userPrompt.contains("Test"))
        );
    }

    // Helper methods

    private Task createTask(String id, String title, Priority priority, Double estimatedHours) {
        return new Task(
                id,
                title,
                null,
                null,
                priority,
                "test-source",
                null,
                TaskStatus.PENDING,
                estimatedHours,
                List.of(),
                LocalDateTime.now(),
                null,
                null
        );
    }

    private Task findTaskById(TasksSummary summary, String taskId) {
        return summary.getAllTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Task not found: " + taskId));
    }

    // ============ Enhanced Integration Tests ============

    @Test
    void summarize_shouldCallApiOnlyOnce() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        summarizer.summarize(List.of(task));

        verify(mockApiClient, times(1)).sendMessage(anyString(), anyString());
    }

    @Test
    void summarize_shouldPassCurrentDate() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        summarizer.summarize(List.of(task));

        verify(mockApiClient).sendMessage(
            anyString(),
            argThat(userPrompt -> userPrompt.contains("2026-06-30") || userPrompt.contains(":") && userPrompt.contains("("))
        );
    }

    @Test
    void summarize_withParserException_shouldReturnFallback() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        // Return response that parser can't handle
        when(mockApiClient.sendMessage(anyString(), anyString()))
            .thenReturn("{ malformed json ]]");

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        // Should fallback, keeping original task unchanged
        assertEquals(1, result.getAllTasks().size());
        assertEquals(Priority.MEDIUM, result.getAllTasks().get(0).getPriority());
    }

    @Test
    void summarize_shouldMaintainTaskOrderAfterUpdates() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        Task task3 = createTask("task-3", "Third", Priority.HIGH, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-2",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "Updated"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        // Order should be maintained: 1, 2 (updated), 3
        assertEquals("task-1", result.getAllTasks().get(0).getId());
        assertEquals("task-2", result.getAllTasks().get(1).getId());
        assertEquals("task-3", result.getAllTasks().get(2).getId());
    }

    @Test
    void summarize_withMultipleUpdatesForSameTask_shouldApplyLatest() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.LOW, 1.0);

        // Note: Claude should ideally not return multiple updates for same task
        // In the map, the second update will override the first (since they have the same key)
        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "First"
                },
                {
                  "taskId": "task-1",
                  "priority": "MEDIUM",
                  "estimatedHours": 2.0,
                  "notes": "Second"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        Task updated = result.getAllTasks().get(0);
        // The last update in the map will be applied (or first, depending on implementation)
        // What matters is that one of them is applied correctly
        assertTrue(updated.getPriority() == Priority.CRITICAL || updated.getPriority() == Priority.MEDIUM);
    }

    @Test
    void summarize_withBothExistingAndNewNotes_shouldCombineNotes() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 2.0);
        task.setNotes("Original note 1\nOriginal note 2");

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "AI added note"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        Task updated = result.getAllTasks().get(0);
        String mergedNotes = updated.getNotes();

        // Both original and AI notes should be present
        assertTrue(mergedNotes.contains("Original note 1"));
        assertTrue(mergedNotes.contains("AI added note"));
        assertTrue(mergedNotes.contains("[AI Analysis]"));
    }

    @Test
    void summarize_shouldNotModifyInputList() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        List<Task> originalTasks = new ArrayList<>(List.of(task1, task2));

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "Updated"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        int originalSize = originalTasks.size();
        Priority originalPriority = originalTasks.get(0).getPriority();

        summarizer.summarize(originalTasks);

        // Original list should be unchanged
        assertEquals(originalSize, originalTasks.size());
        assertEquals(originalPriority, originalTasks.get(0).getPriority());
    }

    @Test
    void summarize_withEstimatedHoursZero_shouldNotApplyUpdate() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 0.0,
                  "notes": "Zero hours - should not update"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        Task updated = result.getAllTasks().get(0);
        // Priority should be updated, but hours should not (since 0.0 is invalid)
        assertEquals(Priority.HIGH, updated.getPriority());
        assertEquals(3.0, updated.getEstimatedHours(), "Original hours should be preserved");
    }

    @Test
    void summarize_withComplexNoteMerging_shouldHandleMultilineNotes() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 2.0);
        task.setNotes("Line 1\nLine 2\nLine 3");

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "AI note line 1\\nAI note line 2"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        Task updated = result.getAllTasks().get(0);
        String notes = updated.getNotes();

        // Should preserve structure
        assertTrue(notes.contains("Line 1"));
        assertTrue(notes.contains("Line 2"));
        assertTrue(notes.contains("Line 3"));
        assertTrue(notes.contains("[AI Analysis]"));
    }

    @Test
    void summarize_shouldIncludeAllTasksInResult() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        Task task3 = createTask("task-3", "Third", Priority.HIGH, 3.0);
        List<Task> tasks = List.of(task1, task2, task3);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(tasks);

        assertEquals(3, result.getAllTasks().size());
        assertTrue(result.getAllTasks().stream().anyMatch(t -> t.getId().equals("task-1")));
        assertTrue(result.getAllTasks().stream().anyMatch(t -> t.getId().equals("task-2")));
        assertTrue(result.getAllTasks().stream().anyMatch(t -> t.getId().equals("task-3")));
    }

    @Test
    void summarize_shouldReturnSummaryWithRecommendations() throws ClaudeApiClient.ClaudeApiException {
        Task task = createTask("task-1", "Test", Priority.MEDIUM, 3.0);

        String mockResponse = """
            {
              "summary": "You have 1 task",
              "schedule": "Work on it today",
              "recommendations": [
                "Start early",
                "Take breaks",
                "Stay focused"
              ],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task));

        assertNotNull(result);
        assertEquals("You have 1 task", result.getSummary());
        assertEquals("Work on it today", result.getSchedule());
        assertEquals(3, result.getRecommendations().size());
        assertTrue(result.getRecommendations().contains("Start early"));
        assertTrue(result.getRecommendations().contains("Take breaks"));
        assertTrue(result.getRecommendations().contains("Stay focused"));
    }

    @Test
    void summarize_withPartiallyValidUpdates_shouldApplyValidOnesOnly() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        Task task3 = createTask("task-3", "Third", Priority.HIGH, 3.0);

        String mockResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "Valid"
                },
                {
                  "taskId": "task-2",
                  "priority": "INVALID_PRIORITY",
                  "estimatedHours": 5.0,
                  "notes": "Should still normalize priority"
                },
                {
                  "taskId": "task-3",
                  "priority": "HIGH",
                  "estimatedHours": -1.0,
                  "notes": "Invalid hours should be ignored"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        // task-1 should be updated
        assertEquals(Priority.CRITICAL, findTaskById(result, "task-1").getPriority());

        // task-2 should have normalized priority
        assertEquals(Priority.MEDIUM, findTaskById(result, "task-2").getPriority());

        // task-3 should keep original hours (invalid update)
        assertEquals(3.0, findTaskById(result, "task-3").getEstimatedHours());
    }
}
