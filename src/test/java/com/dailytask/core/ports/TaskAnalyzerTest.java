package com.dailytask.core.ports;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAnalyzerTest {

    @Mock
    private TaskAnalyzer taskAnalyzer;

    @Test
    void testAnalyzerContract() {
        AnalyzedTasks mockAnalyzed = TestDataBuilder.buildAnalyzedTasks();
        when(taskAnalyzer.analyze(anyList())).thenReturn(mockAnalyzed);

        List<Task> tasksToAnalyze = List.of(TestDataBuilder.buildTask());
        AnalyzedTasks result = taskAnalyzer.analyze(tasksToAnalyze);

        assertNotNull(result);
        assertEquals("Test Summary", result.getSummary());
        assertEquals(1, result.getAllTasks().size());
    }
}