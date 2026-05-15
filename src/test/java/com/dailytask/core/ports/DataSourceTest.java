package com.dailytask.core.ports;

import com.dailytask.adapters.TestDataBuilder;
import com.dailytask.core.domain.RawTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceTest {

    @Mock
    private DataSource dataSource;

    @Test
    void testDataSourceContract() {
        List<RawTask> mockData = List.of(TestDataBuilder.buildRawTask());

        when(dataSource.fetch()).thenReturn(mockData);
        when(dataSource.getName()).thenReturn("MockSource");

        List<RawTask> result = dataSource.fetch();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MockSource", dataSource.getName());
    }
}