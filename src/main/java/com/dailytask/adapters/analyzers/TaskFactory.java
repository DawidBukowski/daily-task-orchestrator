package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class TaskFactory {

    private final DeadlineParser deadlineParser;
    private final TitleExtractor titleExtractor;

    public TaskFactory() {
        this.deadlineParser = new DeadlineParser();
        this.titleExtractor = new TitleExtractor();
    }

    public Task createFromRawData(RawData rawData) {
        if (rawData == null) {
            throw new IllegalArgumentException("RawData cannot be null");
        }

        String id = UUID.randomUUID().toString();
        String title = titleExtractor.extractTitle(rawData);
        String description = rawData.getRawContent() != null ? rawData.getRawContent() : "";
        LocalDateTime deadline = deadlineParser.extractDeadline(rawData.getRawContent(), rawData.getFetchedAt());
        Priority priority = Priority.fromString(rawData.getPriority());
        String source = rawData.getSource();
        String originalId = rawData.getOriginalSource();
        TaskStatus status = TaskStatus.PENDING;
        Double estimatedHours = null;
        ArrayList<String> tags = new ArrayList<>();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        String notes = "";

        return new Task(
                id,
                title,
                description,
                deadline,
                priority,
                source,
                originalId,
                status,
                estimatedHours,
                tags,
                createdAt,
                updatedAt,
                notes
        );
    }
}
