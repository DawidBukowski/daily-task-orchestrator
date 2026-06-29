package com.dailytask.core.usecases;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskExtractor;
import com.dailytask.core.ports.TaskSummarizer;
import com.dailytask.core.ports.TaskNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyTaskOrchestratorTest {

    @Mock private DataSource mockDataSource;
    @Mock private TaskExtractor mockTaskExtractor;
    @Mock private TaskSummarizer mockSummarizer;
    @Mock private TaskNotifier mockNotifier;

    private DailyTaskOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new DailyTaskOrchestrator(
                List.of(mockDataSource),
                mockTaskExtractor,
                mockSummarizer,
                mockNotifier
        );
    }

    @Test
    void shouldExecuteFullWorkflow() {
        // Arrange
        List<RawData> mockRawTasks = List.of(TestDataBuilder.buildRawData());
        List<Task> mockTasks = List.of(TestDataBuilder.buildData());
        TasksSummary mockSummarized = TestDataBuilder.buildSummarizedTasks();

        when(mockDataSource.fetch(any(Instant.class))).thenReturn(mockRawTasks);
        when(mockDataSource.getName()).thenReturn("MockSource");
        when(mockTaskExtractor.extract(mockRawTasks)).thenReturn(mockTasks);
        when(mockSummarizer.summarize(mockTasks)).thenReturn(mockSummarized);

        // Act
        orchestrator.execute();

        // Assert
        verify(mockDataSource, times(1)).fetch(any(Instant.class));
        verify(mockTaskExtractor, times(1)).extract(anyList());
        verify(mockSummarizer, times(1)).summarize(anyList());
        verify(mockNotifier, times(1)).notify(mockSummarized);
    }
}
