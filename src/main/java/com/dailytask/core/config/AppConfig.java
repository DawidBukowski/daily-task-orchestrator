package com.dailytask.core.config;

import com.dailytask.adapters.analyzer.ClaudeTaskAnalyzer;
import com.dailytask.adapters.gmail.GmailDataSource;
import com.dailytask.adapters.notifier.EmailTaskNotifier;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskAnalyzer;
import com.dailytask.core.ports.TaskNotifier;

import java.util.List;

public class AppConfig {

    public static List<DataSource> createDataSources() {
        return List.of(new GmailDataSource());
    }

    public static TaskAnalyzer createAnalyzer() {
        return new ClaudeTaskAnalyzer();
    }

    public static TaskNotifier createNotifier() {
        return new EmailTaskNotifier();
    }
}