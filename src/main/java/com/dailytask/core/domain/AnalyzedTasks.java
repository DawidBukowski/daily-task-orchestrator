package com.dailytask.core.domain;

import java.util.List;

public class AnalyzedTasks {
    private final List<Task> allTasks;
    private final String summary;
    private final String schedule;
    private final List<String> recommendations;

    public AnalyzedTasks(List<Task> allTasks, String summary, String schedule, List<String> recommendations) {
        this.allTasks = allTasks;
        this.summary = summary;
        this.schedule = schedule;
        this.recommendations = recommendations;
    }

    public List<Task> getAllTasks() { return allTasks; }
    public String getSummary() { return summary; }
    public String getSchedule() { return schedule; }
    public List<String> getRecommendations() { return recommendations; }

    @Override
    public String toString() {
        return "AnalyzedTasks{" +
                "tasksCount=" + (allTasks != null ? allTasks.size() : 0) +
                ", summary='" + summary + '\'' +
                '}';
    }
}