package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.ports.TaskNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Email notification adapter implementing TaskNotifier port.
 * Orchestrates email generation and SMTP sending with graceful error handling.
 */
public class EmailTaskNotifier implements TaskNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmailTaskNotifier.class);

    private final EmailConfiguration config;
    private final EmailTemplate emailTemplate;
    private final SmtpEmailSender emailSender;

    public EmailTaskNotifier(
        EmailConfiguration config,
        EmailTemplate emailTemplate,
        SmtpEmailSender emailSender
    ) {
        this.config = Objects.requireNonNull(config, "EmailConfiguration cannot be null");
        this.emailTemplate = Objects.requireNonNull(emailTemplate, "EmailTemplate cannot be null");
        this.emailSender = Objects.requireNonNull(emailSender, "SmtpEmailSender cannot be null");
    }

    @Override
    public void notify(TasksSummary tasks) {
        Objects.requireNonNull(tasks, "TasksSummary cannot be null");

        try {
            logger.info("Generating email for {} tasks", tasks.getAllTasks().size());

            String subject = emailTemplate.generateSubject(tasks);
            String htmlBody = emailTemplate.generateHtml(tasks);

            emailSender.send(subject, htmlBody);

            logger.info("Email notification sent successfully to {}", config.getToEmail());

        } catch (SmtpEmailSender.EmailSendException e) {
            // Graceful degradation: log error but don't crash application
            logger.error("Email notification failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Catch-all for unexpected errors (HTML generation, etc.)
            logger.error("Unexpected error during email notification", e);
        }
    }
}