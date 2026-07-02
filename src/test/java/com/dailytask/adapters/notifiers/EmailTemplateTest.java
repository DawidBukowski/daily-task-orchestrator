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

class EmailTemplateTest {

    private EmailTemplate emailTemplate;
    private HtmlContentBuilder htmlBuilder;

    @BeforeEach
    void setUp() {
        PriorityColorScheme colorScheme = new PriorityColorScheme();
        htmlBuilder = new HtmlContentBuilder(colorScheme);
        emailTemplate = new EmailTemplate(htmlBuilder);
    }

    @Test
    void constructor_withNullHtmlBuilder_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new EmailTemplate(null));
    }

    @Test
    void generateSubject_withNullSummary_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> emailTemplate.generateSubject(null));
    }

    @Test
    void generateSubject_withNoTasks_returnsCorrectFormat() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String subject = emailTemplate.generateSubject(summary);

        assertEquals("Daily Task Summary: 0 tasks", subject);
    }

    @Test
    void generateSubject_withOneTask_usesSingular() {
        Task task = createTask("Task 1", Priority.MEDIUM, LocalDateTime.now().plusDays(1), false);
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String subject = emailTemplate.generateSubject(summary);

        assertEquals("Daily Task Summary: 1 task", subject);
    }

    @Test
    void generateSubject_withMultipleTasks_usesPlural() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", Priority.HIGH, LocalDateTime.now().plusDays(1), false),
            createTask("Task 2", Priority.MEDIUM, LocalDateTime.now().plusDays(2), false)
        );

        TasksSummary summary = new TasksSummary(
            tasks,
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String subject = emailTemplate.generateSubject(summary);

        assertEquals("Daily Task Summary: 2 tasks", subject);
    }

    @Test
    void generateSubject_withOverdueTasks_includesWarningEmoji() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", Priority.HIGH, LocalDateTime.now().minusDays(1), true),
            createTask("Task 2", Priority.MEDIUM, LocalDateTime.now().plusDays(1), false)
        );

        TasksSummary summary = new TasksSummary(
            tasks,
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String subject = emailTemplate.generateSubject(summary);

        assertTrue(subject.startsWith("⚠️ "));
        assertTrue(subject.contains("(1 overdue)"));
    }

    @Test
    void generateSubject_withNoOverdueTasks_noWarningEmoji() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", Priority.HIGH, LocalDateTime.now().plusDays(1), false),
            createTask("Task 2", Priority.MEDIUM, LocalDateTime.now().plusDays(2), false)
        );

        TasksSummary summary = new TasksSummary(
            tasks,
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        String subject = emailTemplate.generateSubject(summary);

        assertFalse(subject.startsWith("⚠️"));
        assertFalse(subject.contains("overdue"));
    }

    @Test
    void generateHtml_withNullSummary_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> emailTemplate.generateHtml(null));
    }

    @Test
    void generateHtml_delegatesToHtmlBuilder() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Test summary",
            "Test schedule",
            Collections.emptyList()
        );

        String html = emailTemplate.generateHtml(summary);

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Test summary"));
        assertTrue(html.contains("Test schedule"));
    }

    private Task createTask(String title, Priority priority, LocalDateTime deadline, boolean overdue) {
        return new Task(
            "1",
            title,
            "Description",
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
