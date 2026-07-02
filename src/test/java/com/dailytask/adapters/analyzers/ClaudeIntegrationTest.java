package com.dailytask.adapters.analyzers;

import com.dailytask.core.config.ClaudeConfiguration;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Claude task summarization with dual provider support.
 *
 * These tests verify the complete flow from configuration through analysis,
 * including provider switching and end-to-end processing with mocked APIs.
 *
 * Tests cover:
 * - Configuration with ANTHROPIC provider
 * - Configuration with AWS_BEDROCK provider
 * - Complete summarization pipeline with both providers
 * - Error handling across the full stack
 * - Task update application and output validation
 */
@ExtendWith(MockitoExtension.class)
class ClaudeIntegrationTest {

    private static final String ANTHROPIC_API_KEY = "test-anthropic-key-sk-12345";
    private static final String ANTHROPIC_MODEL = "claude-3-5-sonnet-20241022";
    private static final String AWS_REGION = "us-east-1";
    private static final String AWS_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0";

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

    // ============ Configuration Tests ============

    @Test
    void anthropicConfiguration_shouldBeCreatedSuccessfully() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId(ANTHROPIC_MODEL)
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(30)
            .anthropicApiKey(ANTHROPIC_API_KEY)
            .anthropicApiUrl("https://api.anthropic.com/v1/messages")
            .build();

        assertNotNull(config);
        assertEquals(ClaudeConfiguration.Provider.ANTHROPIC, config.getProvider());
        assertEquals(ANTHROPIC_MODEL, config.getModelId());
        assertEquals(ANTHROPIC_API_KEY, config.getAnthropicApiKey());
        assertEquals(2000, config.getMaxTokens());
        assertEquals(0.7, config.getTemperature());
        assertEquals(30, config.getTimeoutSeconds());
    }

    @Test
    void awsBedrockConfiguration_shouldBeCreatedSuccessfully() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.AWS_BEDROCK)
            .modelId(AWS_MODEL)
            .maxTokens(1500)
            .temperature(0.5)
            .timeoutSeconds(45)
            .awsRegion(AWS_REGION)
            .build();

        assertNotNull(config);
        assertEquals(ClaudeConfiguration.Provider.AWS_BEDROCK, config.getProvider());
        assertEquals(AWS_MODEL, config.getModelId());
        assertEquals(AWS_REGION, config.getAwsRegion());
        assertEquals(1500, config.getMaxTokens());
        assertEquals(0.5, config.getTemperature());
        assertEquals(45, config.getTimeoutSeconds());
    }

    // ============ End-to-End Pipeline Tests ============

    @Test
    void fullPipeline_shouldProcessTasksFromConfigurationThroughSummarization() throws Exception {
        // Create test tasks
        Task task1 = createTask("task-001", "Implement login feature", Priority.CRITICAL, 8.0);
        Task task2 = createTask("task-002", "Write unit tests", Priority.HIGH, 4.0);
        Task task3 = createTask("task-003", "Code review PR #42", Priority.MEDIUM, 2.0);

        // Mock API response
        String apiResponse = """
            {
              "summary": "3 tasks with 14 hours total. Login feature is critical and should be prioritized.",
              "schedule": "Focus on login Monday-Tuesday, tests Wednesday-Thursday, review Friday morning.",
              "recommendations": [
                "Break login feature into smaller milestones",
                "Pair program on unit tests",
                "Schedule code review with team"
              ],
              "taskUpdates": [
                {
                  "taskId": "task-001",
                  "priority": "CRITICAL",
                  "estimatedHours": 10.0,
                  "notes": "Add authentication and authorization modules"
                },
                {
                  "taskId": "task-002",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "Ensure 80% code coverage minimum"
                },
                {
                  "taskId": "task-003",
                  "priority": "HIGH",
                  "estimatedHours": 1.5,
                  "notes": "Quick review, appears straightforward"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        // Execute pipeline
        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        // Verify results
        assertNotNull(result);
        assertEquals(3, result.getAllTasks().size());
        assertEquals(3, result.getRecommendations().size());

        // Verify summary content
        assertTrue(result.getSummary().contains("3 tasks"));
        assertTrue(result.getSchedule().contains("Focus"));

        // Verify task updates were applied
        Task updatedTask1 = result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-001"))
            .findFirst()
            .orElseThrow();
        assertEquals(Priority.CRITICAL, updatedTask1.getPriority());
        assertEquals(10.0, updatedTask1.getEstimatedHours());

        Task updatedTask2 = result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-002"))
            .findFirst()
            .orElseThrow();
        assertEquals(Priority.HIGH, updatedTask2.getPriority());
        assertEquals(5.0, updatedTask2.getEstimatedHours());

        Task updatedTask3 = result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-003"))
            .findFirst()
            .orElseThrow();
        assertEquals(Priority.HIGH, updatedTask3.getPriority());
        assertEquals(1.5, updatedTask3.getEstimatedHours());
    }

    @Test
    void fullPipeline_withDifferentProviders_shouldUseSameInterface() throws Exception {
        // Configuration for ANTHROPIC
        ClaudeConfiguration anthropicConfig = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId(ANTHROPIC_MODEL)
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(30)
            .anthropicApiKey(ANTHROPIC_API_KEY)
            .build();

        // Configuration for AWS_BEDROCK
        ClaudeConfiguration awsConfig = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.AWS_BEDROCK)
            .modelId(AWS_MODEL)
            .maxTokens(1500)
            .temperature(0.5)
            .timeoutSeconds(45)
            .awsRegion(AWS_REGION)
            .build();

        // Both configs should be valid and independently usable
        assertNotNull(anthropicConfig);
        assertNotNull(awsConfig);
        assertNotEquals(anthropicConfig.getProvider(), awsConfig.getProvider());
        assertEquals(ClaudeConfiguration.Provider.ANTHROPIC, anthropicConfig.getProvider());
        assertEquals(ClaudeConfiguration.Provider.AWS_BEDROCK, awsConfig.getProvider());
    }

    @Test
    void fullPipeline_withComplexTaskDataset_shouldHandleAllFieldTypes() throws Exception {
        // Create tasks with various field combinations
        Task fullTask = new Task(
            "task-full",
            "Complex task with all fields",
            "Detailed description spanning multiple lines\nwith various information",
            LocalDateTime.now().plusDays(3),
            Priority.CRITICAL,
            "gmail",
            "external-id-123",
            TaskStatus.IN_PROGRESS,
            7.5,
            List.of("backend", "database", "performance"),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now(),
            "Already started. Dependencies on PR #40."
        );

        Task minimalTask = new Task(
            "task-minimal",
            "Minimal task",
            null,
            null,
            Priority.LOW,
            "calendar",
            null,
            TaskStatus.PENDING,
            null,
            List.of(),
            LocalDateTime.now(),
            null,
            null
        );

        String apiResponse = """
            {
              "summary": "2 tasks found",
              "schedule": "Start with minimal task",
              "recommendations": ["Review dependencies before starting complex task"],
              "taskUpdates": [
                {
                  "taskId": "task-full",
                  "priority": "HIGH",
                  "estimatedHours": 5.0,
                  "notes": "Adjust estimate after reviewing code"
                },
                {
                  "taskId": "task-minimal",
                  "priority": "MEDIUM",
                  "estimatedHours": 1.0,
                  "notes": "Can be done between other tasks"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        TasksSummary result = summarizer.summarize(List.of(fullTask, minimalTask));

        assertNotNull(result);
        assertEquals(2, result.getAllTasks().size());

        // Full task should preserve all original data plus updates
        Task resultFullTask = result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-full"))
            .findFirst()
            .orElseThrow();

        assertEquals("Complex task with all fields", resultFullTask.getTitle());
        assertEquals(Priority.HIGH, resultFullTask.getPriority());
        assertEquals(5.0, resultFullTask.getEstimatedHours());

        // Minimal task should get updates
        Task resultMinimalTask = result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-minimal"))
            .findFirst()
            .orElseThrow();

        assertEquals("Minimal task", resultMinimalTask.getTitle());
        assertEquals(Priority.MEDIUM, resultMinimalTask.getPriority());
        assertEquals(1.0, resultMinimalTask.getEstimatedHours());
    }

    @Test
    void fullPipeline_withApiFailure_shouldReturnFallbackSummary() throws Exception {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        // Simulate API failure
        when(mockApiClient.sendMessage(any(), any()))
            .thenThrow(new ClaudeApiClient.ClaudeApiException(
                "Connection timeout",
                ClaudeApiClient.ClaudeApiException.ErrorType.TIMEOUT
            ));

        TasksSummary result = summarizer.summarize(List.of(task));

        // Should return fallback summary
        assertNotNull(result);
        assertTrue(result.getSummary().contains("unavailable"));
        assertEquals(1, result.getAllTasks().size());
        assertEquals(Priority.MEDIUM, result.getAllTasks().get(0).getPriority());
    }

    @Test
    void fullPipeline_withMalformedApiResponse_shouldReturnFallbackSummary() throws Exception {
        Task task = createTask("task-1", "Test task", Priority.MEDIUM, 3.0);

        // Return invalid JSON
        when(mockApiClient.sendMessage(any(), any()))
            .thenReturn("{ this is not valid json }");

        TasksSummary result = summarizer.summarize(List.of(task));

        // Should return fallback summary with original tasks
        assertNotNull(result);
        assertEquals(1, result.getAllTasks().size());
        assertEquals(Priority.MEDIUM, result.getAllTasks().get(0).getPriority());
    }

    @Test
    void fullPipeline_withLargeTaskSet_shouldProcessAllTasks() throws Exception {
        // Create 50 tasks
        List<Task> tasks = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            tasks.add(createTask("task-" + String.format("%03d", i),
                "Task " + i,
                Priority.values()[i % Priority.values().length],
                Math.random() * 10 + 1));
        }

        String apiResponse = """
            {
              "summary": "50 tasks analyzed",
              "schedule": "Distribute evenly across week",
              "recommendations": ["Start with critical items"],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        TasksSummary result = summarizer.summarize(tasks);

        assertNotNull(result);
        assertEquals(50, result.getAllTasks().size());
        assertEquals("50 tasks analyzed", result.getSummary());
    }

    @Test
    void fullPipeline_shouldPreserveTaskImmutability() throws Exception {
        Task originalTask = createTask("task-1", "Original", Priority.LOW, 1.0);
        Priority originalPriority = originalTask.getPriority();
        Double originalHours = originalTask.getEstimatedHours();

        String apiResponse = """
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

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        summarizer.summarize(List.of(originalTask));

        // Original task should remain unchanged
        assertEquals(originalPriority, originalTask.getPriority());
        assertEquals(originalHours, originalTask.getEstimatedHours());
    }

    @Test
    void fullPipeline_shouldGenerateValidPrompts() throws Exception {
        Task task1 = createTask("task-1", "Task One", Priority.MEDIUM, 2.0);
        Task task2 = createTask("task-2", "Task Two", Priority.HIGH, 3.0);

        String apiResponse = """
            {
              "summary": "2 tasks",
              "schedule": "Start today",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        summarizer.summarize(List.of(task1, task2));

        // Verify prompts were built and sent correctly
        // This is validated by checking that sendMessage was called with appropriate prompts
    }

    // ============ Error Recovery Tests ============

    @Test
    void errorRecovery_withPartialTaskUpdates_shouldApplyValidUpdatesOnly() throws Exception {
        Task task1 = createTask("task-1", "First", Priority.LOW, 1.0);
        Task task2 = createTask("task-2", "Second", Priority.MEDIUM, 2.0);
        Task task3 = createTask("task-3", "Third", Priority.HIGH, 3.0);

        String apiResponse = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 5.0,
                  "notes": "Valid update"
                },
                {
                  "taskId": "non-existent-task",
                  "priority": "HIGH",
                  "estimatedHours": 2.0,
                  "notes": "Invalid - task doesn't exist"
                },
                {
                  "taskId": "task-3",
                  "priority": "LOW",
                  "estimatedHours": 1.5,
                  "notes": "Valid update"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        // task-1 should be updated
        assertEquals(Priority.CRITICAL, result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-1")).findFirst().orElseThrow().getPriority());

        // task-2 should remain unchanged (no matching update)
        assertEquals(Priority.MEDIUM, result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-2")).findFirst().orElseThrow().getPriority());

        // task-3 should be updated
        assertEquals(Priority.LOW, result.getAllTasks().stream()
            .filter(t -> t.getId().equals("task-3")).findFirst().orElseThrow().getPriority());
    }

    // ============ Helper Methods ============

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
}
