package com.dailytask.core.usecases;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.RawTask;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskAnalyzer;
import com.dailytask.core.ports.TaskNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyTaskOrchestratorTest {

    @Mock private DataSource mockDataSource;
    @Mock private TaskAnalyzer mockAnalyzer;
    @Mock private TaskNotifier mockNotifier;

    private DailyTaskOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new DailyTaskOrchestrator(
                List.of(mockDataSource),
                mockAnalyzer,
                mockNotifier
        );
    }

    @Test
    void shouldExecuteFullWorkflow() {
        // Arrange
        List<RawTask> mockRawTasks = List.of(TestDataBuilder.buildRawTask());
        AnalyzedTasks mockAnalyzed = TestDataBuilder.buildAnalyzedTasks();

        when(mockDataSource.fetch()).thenReturn(mockRawTasks);
        when(mockDataSource.getName()).thenReturn("MockSource");
        when(mockAnalyzer.analyze(any())).thenReturn(mockAnalyzed);

        // Act
        orchestrator.execute();

        // Assert
        verify(mockDataSource, times(1)).fetch();
        verify(mockAnalyzer, times(1)).analyze(anyList());
        verify(mockNotifier, times(1)).notify(mockAnalyzed);
    }
}