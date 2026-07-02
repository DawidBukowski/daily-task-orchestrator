package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.TasksSummary;

import java.util.Objects;

/**
 * Facade for coordinating email template generation.
 * Generates subject lines and HTML body content for task summary emails.
 */
public class EmailTemplate {

    private final HtmlContentBuilder htmlBuilder;

    public EmailTemplate(HtmlContentBuilder htmlBuilder) {
        this.htmlBuilder = Objects.requireNonNull(htmlBuilder, "HtmlContentBuilder cannot be null");
    }

    /**
     * Generates email subject line from task summary.
     * Format: "Daily Task Summary: X tasks (Y overdue)" or "⚠️ Daily Task Summary: X tasks (Y overdue)"
     *
     * @param summary the task summary
     * @return formatted subject line
     */
    public String generateSubject(TasksSummary summary) {
        Objects.requireNonNull(summary, "TasksSummary cannot be null");

        int taskCount = summary.getAllTasks().size();
        int overdueCount = summary.getOverdueTasks().size();

        StringBuilder subject = new StringBuilder();

        // Add warning emoji if there are overdue tasks
        if (overdueCount > 0) {
            subject.append("⚠️ ");
        }

        subject.append("Daily Task Summary: ")
               .append(taskCount)
               .append(" task");

        // Pluralize
        if (taskCount != 1) {
            subject.append("s");
        }

        // Add overdue count if present
        if (overdueCount > 0) {
            subject.append(" (").append(overdueCount).append(" overdue)");
        }

        return subject.toString();
    }

    /**
     * Generates HTML email body from task summary.
     *
     * @param summary the task summary
     * @return complete HTML document
     */
    public String generateHtml(TasksSummary summary) {
        Objects.requireNonNull(summary, "TasksSummary cannot be null");
        return htmlBuilder.buildHtml(summary);
    }
}
