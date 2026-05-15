package com.dailytask.adapters.notifier;

import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.ports.TaskNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailTaskNotifier implements TaskNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmailTaskNotifier.class);

    @Override
    public void notify(AnalyzedTasks tasks) {
        logger.info("Sending notification email to user...");
        // TODO: Implement actual email sending logic
        logger.debug("Notification payload: {}", tasks.getSummary());
    }
}