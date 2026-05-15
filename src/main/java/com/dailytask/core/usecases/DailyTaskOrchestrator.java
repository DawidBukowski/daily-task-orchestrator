package com.dailytask.core.usecases;

import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.RawTask;
import com.dailytask.core.domain.Task;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskAnalyzer;
import com.dailytask.core.ports.TaskNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DailyTaskOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(DailyTaskOrchestrator.class);

    private final List<DataSource> dataSources;
    private final TaskAnalyzer analyzer;
    private final TaskNotifier notifier;

    public DailyTaskOrchestrator(List<DataSource> dataSources, TaskAnalyzer analyzer, TaskNotifier notifier) {
        this.dataSources = dataSources;
        this.analyzer = analyzer;
        this.notifier = notifier;
    }

    public void execute() {
        logger.info("Starting Daily Task Orchestration...");

        try {
            List<RawTask> allRawTasks = new ArrayList<>();
            for (DataSource source : dataSources) {
                logger.debug("Fetching from source: {}", source.getName());
                allRawTasks.addAll(source.fetch());
            }

            // Normalization step (mocked for now)
            List<Task> normalizedTasks = normalizeTasks(allRawTasks);

            AnalyzedTasks analyzedResult = analyzer.analyze(normalizedTasks);

            notifier.notify(analyzedResult);

            logger.info("Daily Task Orchestration completed successfully.");
        } catch (Exception e) {
            logger.error("Failed to execute daily task orchestration", e);
        }
    }

    private List<Task> normalizeTasks(List<RawTask> rawTasks) {
        // TODO: Extract actual normalization logic to a dedicated mapper/service
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < rawTasks.size(); i++) {
            RawTask raw = rawTasks.get(i);
            tasks.add(new Task(
                    "T-" + i,
                    raw.getTitle(),
                    raw.getRawContent(),
                    LocalDateTime.now().plusDays(1),
                    "MEDIUM",
                    raw.getSource(),
                    "PENDING"
            ));
        }
        return tasks;
    }
}