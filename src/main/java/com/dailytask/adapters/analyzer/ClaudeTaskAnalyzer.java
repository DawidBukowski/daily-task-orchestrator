package com.dailytask.adapters.analyzer;

import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.Task;
import com.dailytask.core.ports.TaskAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClaudeTaskAnalyzer implements TaskAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeTaskAnalyzer.class);

    @Override
    public AnalyzedTasks analyze(List<Task> tasks) {
        logger.info("Analyzing {} tasks using Claude AI...", tasks.size());
        // TODO: Implement actual Claude AI API call
        return new AnalyzedTasks(
                tasks,
                "Dummy Summary: You have " + tasks.size() + " tasks pending.",
                "Dummy Schedule: Do everything ASAP.",
                new ArrayList<>()
        );
    }
}