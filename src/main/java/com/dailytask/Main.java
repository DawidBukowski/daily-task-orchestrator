package com.dailytask;

import com.dailytask.core.config.AppConfig;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskExtractor;
import com.dailytask.core.ports.TaskSummarizer;
import com.dailytask.core.ports.TaskNotifier;
import com.dailytask.core.usecases.DailyTaskOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Initializing Application Dependencies...");

        try {
            List<DataSource> sources = AppConfig.createDataSources();
            TaskExtractor extractor = AppConfig.createTaskExtractor();
            TaskSummarizer analyzer = AppConfig.createAnalyzer();
            TaskNotifier notifier = AppConfig.createNotifier();

            DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(sources, extractor, analyzer, notifier);

            orchestrator.execute();

        } catch (Exception e) {
            logger.error("Application crashed during initialization or execution", e);
            System.exit(1);
        }
    }
}