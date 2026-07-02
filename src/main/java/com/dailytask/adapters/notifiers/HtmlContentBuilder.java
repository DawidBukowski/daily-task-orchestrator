package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TasksSummary;
import org.apache.commons.text.StringEscapeUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Generates HTML email content for task summaries.
 * Uses inline CSS for maximum email client compatibility.
 */
public class HtmlContentBuilder {

    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PriorityColorScheme colorScheme;

    public HtmlContentBuilder(PriorityColorScheme colorScheme) {
        this.colorScheme = Objects.requireNonNull(colorScheme, "PriorityColorScheme cannot be null");
    }

    /**
     * Builds complete HTML email body from TasksSummary.
     *
     * @param summary the task summary to render
     * @return complete HTML document
     */
    public String buildHtml(TasksSummary summary) {
        Objects.requireNonNull(summary, "TasksSummary cannot be null");

        StringBuilder html = new StringBuilder();
        html.append(buildHtmlHeader());
        html.append(buildSummarySection(summary.getSummary()));
        html.append(buildTasksSection(summary.getTasksSortedByPriority()));
        html.append(buildScheduleSection(summary.getSchedule()));

        if (summary.getRecommendations() != null && !summary.getRecommendations().isEmpty()) {
            html.append(buildRecommendationsSection(summary.getRecommendations()));
        }

        html.append(buildHtmlFooter());
        return html.toString();
    }

    private String buildHtmlHeader() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Daily Task Summary</title>
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                         max-width: 800px; margin: 0 auto; padding: 20px; background-color: #f8f9fa;">
            """;
    }

    private String buildSummarySection(String summary) {
        return String.format("""
            <div style="background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
              <h2 style="margin-top: 0; color: #212529;">📋 Daily Task Summary</h2>
              <p style="color: #495057; line-height: 1.6;">%s</p>
            </div>
            """, escape(summary));
    }

    private String buildTasksSection(List<Task> tasks) {
        StringBuilder section = new StringBuilder();
        section.append(String.format("""
            <div style="background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
              <h3 style="margin-top: 0; color: #212529;">Tasks (%d)</h3>
            """, tasks.size()));

        for (Task task : tasks) {
            section.append(buildTaskCard(task));
        }

        section.append("</div>");
        return section.toString();
    }

    private String buildTaskCard(Task task) {
        String priorityColor = colorScheme.getColor(task.getPriority());
        String priorityTextColor = colorScheme.getTextColor(task.getPriority());
        String priorityName = task.getPriority() != null ? task.getPriority().name() : "MEDIUM";

        StringBuilder card = new StringBuilder();
        card.append(String.format("""
            <div style="border-left: 4px solid %s; padding: 12px; margin: 10px 0; background: #f8f9fa; border-radius: 4px;">
              <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap;">
                <strong style="color: #212529; font-size: 16px;">%s</strong>
                <span style="background: %s; color: %s; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; white-space: nowrap;">
                  %s
                </span>
              </div>
            """, priorityColor, escape(task.getTitle()), priorityColor, priorityTextColor, priorityName));

        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            card.append(String.format("""
              <p style="color: #6c757d; margin: 8px 0; line-height: 1.5;">%s</p>
            """, escape(task.getDescription())));
        }

        // Deadline section
        card.append("<div style=\"font-size: 14px; color: #495057; margin-top: 8px;\">");
        if (task.getDeadline() != null) {
            if (task.isOverdue()) {
                card.append(String.format("""
                  <span style="color: #dc3545; font-weight: bold;">⚠️ OVERDUE: %s</span>
                """, task.getDeadline().format(DATETIME_FORMATTER)));
            } else {
                long daysUntil = task.daysUntilDue();
                String daysText = daysUntil == 1 ? "day" : "days";
                card.append(String.format("""
                  📅 Due: %s (%d %s)
                """, task.getDeadline().format(DATETIME_FORMATTER), daysUntil, daysText));
            }

            // Gmail link if available
            String gmailLink = task.getGmailLink();
            if (gmailLink != null) {
                card.append(String.format("""
                  (<a href="%s" target="_blank" style="color: #007bff; text-decoration: none;">View in Gmail</a>)
                """, gmailLink));
            }
        } else {
            card.append("📅 No deadline set");
        }
        card.append("</div>");

        // Notes section
        if (task.getNotes() != null && !task.getNotes().isBlank()) {
            card.append(String.format("""
              <div style="margin-top: 8px; padding: 8px; background: #ffffff; border-radius: 4px; border: 1px solid #dee2e6;">
                <span style="color: #6c757d; font-size: 13px;">📝 %s</span>
              </div>
            """, escape(task.getNotes())));
        }

        card.append("</div>");
        return card.toString();
    }

    private String buildScheduleSection(String schedule) {
        return String.format("""
            <div style="background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
              <h3 style="margin-top: 0; color: #212529;">📅 Recommended Schedule</h3>
              <pre style="background: #f8f9fa; padding: 12px; border-radius: 4px; white-space: pre-wrap;
                          font-family: 'Courier New', monospace; font-size: 14px; color: #495057; overflow-x: auto;">%s</pre>
            </div>
            """, escape(schedule));
    }

    private String buildRecommendationsSection(List<String> recommendations) {
        StringBuilder section = new StringBuilder();
        section.append("""
            <div style="background: white; border: 2px solid #28a745; border-radius: 8px; padding: 20px;
                        margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
              <h3 style="color: #28a745; margin-top: 0;">💡 Recommendations</h3>
              <ul style="color: #495057; line-height: 1.6; padding-left: 20px;">
            """);

        for (String recommendation : recommendations) {
            section.append(String.format("<li>%s</li>%n", escape(recommendation)));
        }

        section.append("""
              </ul>
            </div>
            """);
        return section.toString();
    }

    private String buildHtmlFooter() {
        String currentDateTime = LocalDateTime.now().format(DATETIME_FORMATTER);
        return String.format("""
            <div style="text-align: center; color: #6c757d; font-size: 12px; margin-top: 20px; padding-top: 20px;
                        border-top: 1px solid #dee2e6;">
              Generated on %s by Daily Task Orchestrator
            </div>
            </body>
            </html>
            """, currentDateTime);
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     *
     * @param text the text to escape
     * @return escaped text safe for HTML rendering
     */
    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return StringEscapeUtils.escapeHtml4(text);
    }
}
