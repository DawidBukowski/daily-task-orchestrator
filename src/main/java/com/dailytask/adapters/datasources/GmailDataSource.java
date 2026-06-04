package com.dailytask.adapters.datasources;

import com.dailytask.adapters.datasources.gmail.GmailApiClient;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.ports.DataSource;
import com.google.api.services.gmail.model.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GmailDataSource implements DataSource {
    private static final Logger logger = Logger.getLogger(GmailDataSource.class.getName());
    private final GmailApiClient apiClient;

    public GmailDataSource(GmailApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<RawData> fetch(Instant from) {
        logger.info("Starting fetch from Gmail since: " + from);

        long epochSeconds = from.getEpochSecond();
        String query = "after:" + epochSeconds;

        try {
            List<Message> rawMessages = apiClient.getEmails(query, 100L);

            return rawMessages.stream()
                    .map(this::mapToRawData)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.severe("Error during Gmail fetch operation: " + e.getMessage());
            throw new RuntimeException("Failed to extract RawData from Gmail", e);
        }
    }

    @Override
    public String getName() {
        return "Gmail";
    }

    private RawData mapToRawData(Message message) {
        String content = message.getSnippet(); // Simplified for now. Full body extraction is complex due to MIME parts.
        String sourceId = message.getId();
        Instant timestamp = Instant.ofEpochMilli(message.getInternalDate());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());


        // Assuming RawData constructor: RawData(String source, String externalId, String rawContent, Instant timestamp)
        return new RawData(getName(), sourceId, content, localDateTime);
    }
}