package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import com.dailytask.core.domain.TasksSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlContentBuilderTest {

    private HtmlContentBuilder builder;
    private PriorityColorScheme colorScheme;

    @BeforeEach
    void setUp() {
        colorScheme = new PriorityColorScheme();
        builder = new HtmlContentBuilder(colorScheme);
    }

    @Test
    void constructor_withNullColorScheme_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new HtmlContentBuilder(null));
    }

    @Test
    void buildHtml_withNullSummary_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> builder.buildHtml(null));
    }

    @Test
    void buildHtml_containsHtmlStructure() {
        TasksSummary summary = createMinimalSummary();

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<head>"));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
        assertTrue(html.contains("<meta name=\"viewport\""));
        assertTrue(html.contains("</body>"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void buildHtml_containsSummarySection() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Test summary content",
            "Test schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("📋 Daily Task Summary"));
        assertTrue(html.contains("Test summary content"));
    }

    @Test
    void buildHtml_containsScheduleSection() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "9:00 AM - Morning tasks\n10:00 AM - Afternoon tasks",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("📅 Recommended Schedule"));
        assertTrue(html.contains("9:00 AM - Morning tasks"));
        assertTrue(html.contains("10:00 AM - Afternoon tasks"));
    }

    @Test
    void buildHtml_withRecommendations_containsRecommendationsSection() {
        List<String> recommendations = Arrays.asList(
            "Focus on critical tasks first",
            "Take breaks every 2 hours"
        );

        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "Schedule",
            recommendations
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("💡 Recommendations"));
        assertTrue(html.contains("Focus on critical tasks first"));
        assertTrue(html.contains("Take breaks every 2 hours"));
    }

    @Test
    void buildHtml_withoutRecommendations_omitsRecommendationsSection() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertFalse(html.contains("💡 Recommendations"));
    }

    @Test
    void buildHtml_withNullRecommendations_omitsRecommendationsSection() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "Schedule",
            null
        );

        String html = builder.buildHtml(summary);

        assertFalse(html.contains("💡 Recommendations"));
    }

    @Test
    void buildHtml_containsTasksSection() {
        Task task = createTask("Test Task", Priority.HIGH, LocalDateTime.now().plusDays(2));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("Tasks (1)"));
        assertTrue(html.contains("Test Task"));
    }

    @Test
    void buildHtml_withOverdueTask_showsOverdueWarning() {
        Task overdueTask = createTask("Overdue Task", Priority.CRITICAL, LocalDateTime.now().minusDays(1));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(overdueTask),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("⚠️ OVERDUE"));
        assertTrue(html.contains("color: #dc3545"));
    }

    @Test
    void buildHtml_withFutureDeadline_showsDaysUntilDue() {
        Task task = createTask("Future Task", Priority.MEDIUM, LocalDateTime.now().plusDays(3));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("📅 Due:"));
        assertTrue(html.contains("days") || html.contains("day"));
    }

    @Test
    void buildHtml_withGmailLink_containsGmailUrl() {
        Task task = new Task(
            "1",
            "Email Task",
            "Description",
            LocalDateTime.now().plusDays(1),
            Priority.HIGH,
            "Gmail",
            "18f3c4a5b2d8e9f1",
            TaskStatus.PENDING,
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );

        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("View in Gmail"));
        assertTrue(html.contains("https://mail.google.com/mail/u/0/#inbox/18f3c4a5b2d8e9f1"));
    }

    @Test
    void buildHtml_escapesHtmlInTaskTitle() {
        Task task = createTask("<script>alert('XSS')</script>", Priority.LOW, LocalDateTime.now().plusDays(1));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void buildHtml_escapesHtmlInSummary() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary with <b>HTML</b> & special chars",
            "Schedule",
            Collections.emptyList()
        );

        String html = builder.buildHtml(summary);

        assertFalse(html.contains("<b>"));
        assertTrue(html.contains("&lt;b&gt;"));
        assertTrue(html.contains("&amp;"));
    }

    @Test
    void buildHtml_containsFooterWithTimestamp() {
        TasksSummary summary = createMinimalSummary();

        String html = builder.buildHtml(summary);

        assertTrue(html.contains("Generated on"));
        assertTrue(html.contains("Daily Task Orchestrator"));
    }

    private TasksSummary createMinimalSummary() {
        return new TasksSummary(
            Collections.emptyList(),
            "Minimal summary",
            "Minimal schedule",
            Collections.emptyList()
        );
    }

    private Task createTask(String title, Priority priority, LocalDateTime deadline) {
        return new Task(
            "1",
            title,
            "Test description",
            deadline,
            priority,
            "Test",
            null,
            TaskStatus.PENDING,
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );
    }
}
