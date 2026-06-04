package com.dailytask.core.ports;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.RawData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceTest {

    @Mock
    private DataSource dataSource;

    @Test
    void testDataSourceContract() {
        List<RawData> mockData = List.of(TestDataBuilder.buildRawData());

        Instant targetTime = Instant.now().minusSeconds(24 * 60 * 60); // <-- Zmienna lokalna

        when(dataSource.fetch(targetTime)).thenReturn(mockData);
        when(dataSource.getName()).thenReturn("MockSource");

        List<RawData> result = dataSource.fetch(targetTime);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MockSource", dataSource.getName());
    }
}