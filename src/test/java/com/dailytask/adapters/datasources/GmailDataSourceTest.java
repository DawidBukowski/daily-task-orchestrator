package com.dailytask.adapters.datasources;

import com.dailytask.adapters.datasources.gmail.GmailApiClient;
import com.dailytask.core.domain.RawData;
import com.google.api.services.gmail.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GmailDataSourceTest {

    private GmailApiClient mockApiClient;
    private GmailDataSource gmailDataSource;

    @BeforeEach
    void setUp() {
        mockApiClient = mock(GmailApiClient.class);
        gmailDataSource = new GmailDataSource(mockApiClient);
    }

    @Test
    void fetch_ReturnsListOfRawData_WhenApiSucceeds() {
        Instant from = Instant.parse("2023-10-01T12:00:00Z");
        String expectedQuery = "after:" + from.getEpochSecond();

        Message msg1 = new Message().setId("1").setSnippet("Test 1").setInternalDate(from.toEpochMilli());
        Message msg2 = new Message().setId("2").setSnippet("Test 2").setInternalDate(from.toEpochMilli() + 1000);

        when(mockApiClient.getEmails(eq(expectedQuery), anyLong())).thenReturn(List.of(msg1, msg2));

        List<RawData> result = gmailDataSource.fetch(from);

        assertEquals(2, result.size());
        assertEquals("Gmail", result.get(0).getSource());
        assertEquals("1", result.get(0).getTitle());
        assertEquals("Test 1", result.get(0).getRawContent());

        verify(mockApiClient, times(1)).getEmails(expectedQuery, 100L);
    }

    @Test
    void fetch_ThrowsException_WhenApiFails() {
        Instant from = Instant.now();
        when(mockApiClient.getEmails(anyString(), anyLong())).thenThrow(new RuntimeException("API Down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> gmailDataSource.fetch(from));
        assertTrue(exception.getMessage().contains("Failed to extract RawData"));
    }
}