package com.dailytask.core.ports;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.adapters.analyzers.ClaudeRawDataAnalyzer;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TaskExtractorTest {

    @Disabled
    @Test
    void testExtractorContract() {
        TaskExtractor taskExtractor = new ClaudeRawDataAnalyzer();

        List<RawData> rawData = List.of(TestDataBuilder.buildRawData());
        List<Task> result = taskExtractor.extract(rawData);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertEquals("Test Email", result.get(0).getTitle());
    }
}