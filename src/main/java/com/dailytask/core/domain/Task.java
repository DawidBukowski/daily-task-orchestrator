package com.dailytask.core.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Task {
    private String id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Priority priority;
    private String source;
    private String originalId;
    private TaskStatus status;
    private Double estimatedHours;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;

    public Task(String id, String title, String description, LocalDateTime deadline,
                Priority priority, String source, String originalId, TaskStatus status,
                Double estimatedHours, List<String> tags, LocalDateTime createdAt,
                LocalDateTime updatedAt, String notes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.source = source;
        this.originalId = originalId;
        this.status = status;
        this.estimatedHours = estimatedHours;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notes = notes;
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

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getOriginalId() { return originalId; }
    public void setOriginalId(String originalId) { this.originalId = originalId; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }

    public List<String> getTags() { return new ArrayList<>(tags); }
    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isOverdue() {
        if (deadline == null) {
            return false;
        }
        return deadline.isBefore(LocalDateTime.now());
    }

    public long daysUntilDue() {
        if (deadline == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
               Objects.equals(title, task.title) &&
               Objects.equals(description, task.description) &&
               Objects.equals(deadline, task.deadline) &&
               priority == task.priority &&
               Objects.equals(source, task.source) &&
               Objects.equals(originalId, task.originalId) &&
               status == task.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, deadline, priority, source, originalId, status);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", deadline=" + deadline +
                ", source='" + source + '\'' +
                '}';
    }
}
