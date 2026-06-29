package com.dailytask.core.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TasksSummary {
    private final List<Task> allTasks;
    private final String summary;
    private final String schedule;
    private final List<String> recommendations;

    public TasksSummary(List<Task> allTasks, String summary, String schedule, List<String> recommendations) {
        this.allTasks = allTasks;
        this.summary = summary;
        this.schedule = schedule;
        this.recommendations = recommendations;
    }

    public List<Task> getAllTasks() { return allTasks; }
    public String getSummary() { return summary; }
    public String getSchedule() { return schedule; }
    public List<String> getRecommendations() { return recommendations; }

    public List<Task> getTasksSortedByPriority() {
        if (allTasks == null) {
            return List.of();
        }
        return allTasks.stream()
                .sorted(Comparator.comparingInt((Task t) -> t.getPriority().getNumericValue()).reversed())
                .collect(Collectors.toList());
    }

    public List<Task> getTodaysTasks() {
        if (allTasks == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return allTasks.stream()
                .filter(task -> task.getDeadline() != null && task.getDeadline().toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueTasks() {
        if (allTasks == null) {
            return List.of();
        }
        return allTasks.stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "TasksSummary{" +
                "tasksCount=" + (allTasks != null ? allTasks.size() : 0) +
                ", summary='" + summary + '\'' +
                '}';
    }
}
