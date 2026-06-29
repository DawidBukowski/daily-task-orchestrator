package com.dailytask.core.usecases;

import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskExtractor;
import com.dailytask.core.ports.TaskSummarizer;
import com.dailytask.core.ports.TaskNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

public class DailyTaskOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(DailyTaskOrchestrator.class);

    private final List<DataSource> dataSources;
    private final TaskExtractor taskExtractor;
    private final TaskSummarizer summarizer;
    private final TaskNotifier notifier;

    public DailyTaskOrchestrator(List<DataSource> dataSources, TaskExtractor taskExtractor,
                                 TaskSummarizer summarizer, TaskNotifier notifier) {
        this.dataSources = dataSources;
        this.taskExtractor = taskExtractor;
        this.summarizer = summarizer;
        this.notifier = notifier;
    }

    public void execute() {
        logger.info("Starting Daily Task Orchestration...");

        try {
            List<RawData> allRawTasks = new ArrayList<>();
            for (DataSource source : dataSources) {
                logger.debug("Fetching from source: {}", source.getName());
                allRawTasks.addAll(source.fetch(Instant.now().minusSeconds(24 * 3600))); // Fetch tasks from the last 24 hours
            }

            List<Task> normalizedTasks = taskExtractor.extract(allRawTasks);

            TasksSummary analyzedResult = summarizer.summarize(normalizedTasks);

            notifier.notify(analyzedResult);

            logger.info("Daily Task Orchestration completed successfully.");
        } catch (Exception e) {
            logger.error("Failed to execute daily task orchestration", e);
        }
    }
}