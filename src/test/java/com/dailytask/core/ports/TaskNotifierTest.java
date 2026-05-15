package com.dailytask.core.ports;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.AnalyzedTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskNotifierTest {

    @Mock
    private TaskNotifier taskNotifier;

    @Test
    void testNotifierContract() {
        AnalyzedTasks payload = TestDataBuilder.buildAnalyzedTasks();

        taskNotifier.notify(payload);

        // Verify the method is callable with the domain object
        verify(taskNotifier).notify(payload);
    }
}