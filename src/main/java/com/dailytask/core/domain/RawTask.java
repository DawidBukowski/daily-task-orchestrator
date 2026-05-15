package com.dailytask.core.domain;

import java.time.LocalDateTime;

public class RawTask {
    private final String source;
    private final String title;
    private final String rawContent;
    private final LocalDateTime fetchedAt;

    public RawTask(String source, String title, String rawContent, LocalDateTime fetchedAt) {
        this.source = source;
        this.title = title;
        this.rawContent = rawContent;
        this.fetchedAt = fetchedAt;
    }

    public String getSource() { return source; }
    public String getTitle() { return title; }
    public String getRawContent() { return rawContent; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }

    @Override
    public String toString() {
        return "RawTask{" +
                "source='" + source + '\'' +
                ", title='" + title + '\'' +
                ", fetchedAt=" + fetchedAt +
                '}';
    }
}