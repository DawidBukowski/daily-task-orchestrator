package com.dailytask.core.domain;

import java.time.LocalDateTime;

public class Task {
    private String id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private String source;
    private String status;

    public Task(String id, String title, String description, LocalDateTime deadline,
                String priority, String source, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.source = source;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}