package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import com.dailytask.core.domain.TasksSummary;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for EmailTaskNotifier using GreenMail in-memory SMTP server.
 * Verifies the complete email sending flow from EmailTaskNotifier to SMTP delivery.
 */
class EmailTaskNotifierIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
        .withConfiguration(GreenMailConfiguration.aConfig().withUser("test@example.com", "password"))
        .withPerMethodLifecycle(true);  // Reset after each test method

    private EmailTaskNotifier notifier;
    private EmailConfiguration config;

    @BeforeEach
    void setUp() {
        // Configure EmailTaskNotifier to use GreenMail's SMTP server
        config = EmailConfiguration.builder()
            .smtpHost(greenMail.getSmtp().getBindTo())
            .smtpPort(greenMail.getSmtp().getPort())
            .username("test@example.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .enableTls(false)  // GreenMail doesn't need TLS
            .enableAuth(true)
            .build();

        // Wire all dependencies
        PriorityColorScheme colorScheme = new PriorityColorScheme();
        HtmlContentBuilder htmlBuilder = new HtmlContentBuilder(colorScheme);
        EmailTemplate emailTemplate = new EmailTemplate(htmlBuilder);
        SmtpEmailSender emailSender = new SmtpEmailSender(config);

        notifier = new EmailTaskNotifier(config, emailTemplate, emailSender);
    }

    @Test
    void notify_sendsHtmlEmailWithCorrectContent() throws Exception {
        // Arrange: Create task summary with multiple tasks
        Task criticalTask = createTask(
            "Fix production outage",
            "Critical database connection failure",
            Priority.CRITICAL,
            LocalDateTime.now().plusHours(2),
            TaskStatus.PENDING,
            "msg-001"
        );

        Task highTask = createTask(
            "Deploy new feature",
            "Deploy user authentication module",
            Priority.HIGH,
            LocalDateTime.now().plusDays(1),
            TaskStatus.IN_PROGRESS,
            "msg-002"
        );

        Task mediumTask = createTask(
            "Update documentation",
            "Update API documentation for v2.0",
            Priority.MEDIUM,
            LocalDateTime.now().plusDays(3),
            TaskStatus.PENDING,
            "msg-003"
        );

        List<Task> tasks = Arrays.asList(criticalTask, highTask, mediumTask);

        TasksSummary summary = new TasksSummary(
            tasks,
            "You have 3 tasks requiring attention. 1 critical task needs immediate action.",
            "Morning: Focus on critical database issue. Afternoon: Continue feature deployment. Evening: Update documentation.",
            Arrays.asList(
                "Prioritize the database outage - it's blocking production",
                "Consider allocating more resources to the authentication deployment"
            )
        );

        // Act: Send notification
        notifier.notify(summary);

        // Assert: Verify email was received
        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "Should receive exactly 1 email");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length, "Should receive exactly 1 email");

        MimeMessage message = messages[0];

        // Verify headers
        assertEquals("from@example.com", message.getFrom()[0].toString());
        assertTrue(message.getAllRecipients()[0].toString().contains("to@example.com"));

        // Verify subject contains task count and overdue warning
        String subject = message.getSubject();
        assertTrue(subject.contains("Daily Task Summary"), "Subject should contain 'Daily Task Summary'");
        assertTrue(subject.contains("3 task"), "Subject should contain task count");

        // Verify HTML body (decode quoted-printable encoding)
        String body = GreenMailUtil.getBody(message).replaceAll("=\\r?\\n", "").replace("=3D", "=");

        // Check HTML structure
        assertTrue(body.contains("<!DOCTYPE html>"), "Should have DOCTYPE declaration");
        assertTrue(body.contains("<html"), "Should have HTML tag");
        assertTrue(body.contains("UTF-8"), "Should specify UTF-8 charset");

        // Check summary section
        assertTrue(body.contains("3 tasks requiring attention"), "Should contain summary text");

        // Check all tasks are present
        assertTrue(body.contains("Fix production outage"), "Should contain critical task title");
        assertTrue(body.contains("Deploy new feature"), "Should contain high priority task title");
        assertTrue(body.contains("Update documentation"), "Should contain medium priority task title");

        // Check priority badges
        assertTrue(body.contains("CRITICAL"), "Should contain CRITICAL badge");
        assertTrue(body.contains("HIGH"), "Should contain HIGH badge");
        assertTrue(body.contains("MEDIUM"), "Should contain MEDIUM badge");

        // Check schedule section
        assertTrue(body.contains("Recommended Schedule"), "Should contain schedule section");
        assertTrue(body.contains("Morning: Focus on critical database issue"), "Should contain schedule text");

        // Check recommendations section
        assertTrue(body.contains("Recommendations"), "Should contain recommendations section");
        assertTrue(body.contains("Prioritize the database outage"), "Should contain first recommendation");
        assertTrue(body.contains("Consider allocating more resources"), "Should contain second recommendation");

        // Check Gmail links (if originalId present)
        assertTrue(body.contains("https://mail.google.com/mail/u/0/#inbox/msg-001"), "Should contain Gmail link for critical task");

        // Check content type is HTML
        assertTrue(message.getContentType().contains("text/html"), "Content type should be text/html");
    }

    @Test
    void notify_withOverdueTask_includesOverdueWarningInSubject() throws Exception {
        // Arrange: Create overdue task
        Task overdueTask = createTask(
            "Overdue assignment",
            "Submit project report",
            Priority.HIGH,
            LocalDateTime.now().minusDays(2),  // 2 days overdue
            TaskStatus.PENDING,
            null
        );

        TasksSummary summary = new TasksSummary(
            Collections.singletonList(overdueTask),
            "You have 1 overdue task.",
            "Complete the overdue assignment immediately.",
            Collections.emptyList()
        );

        // Act
        notifier.notify(summary);

        // Assert
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage message = greenMail.getReceivedMessages()[0];

        String subject = message.getSubject();
        assertTrue(subject.contains("⚠️"), "Subject should contain warning emoji for overdue task");
        assertTrue(subject.contains("1 overdue"), "Subject should mention overdue count");

        String body = GreenMailUtil.getBody(message).replaceAll("=\\r?\\n", "").replace("=3D", "=");
        assertTrue(body.contains("OVERDUE"), "Body should contain overdue warning");
    }

    @Test
    void notify_withNoRecommendations_omitsRecommendationsSection() throws Exception {
        // Arrange: Summary without recommendations
        Task task = createTask(
            "Simple task",
            "No recommendations needed",
            Priority.LOW,
            LocalDateTime.now().plusDays(7),
            TaskStatus.PENDING,
            null
        );

        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "One simple task.",
            "Complete at your convenience.",
            Collections.emptyList()  // No recommendations
        );

        // Act
        notifier.notify(summary);

        // Assert
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage message = greenMail.getReceivedMessages()[0];

        String body = GreenMailUtil.getBody(message).replaceAll("=\\r?\\n", "").replace("=3D", "=");

        // Recommendations section should NOT be present
        assertFalse(body.contains("💡 Recommendations"), "Should not contain recommendations section when list is empty");
    }

    @Test
    void notify_withEmptyTaskList_sendsEmailWithZeroTasks() throws Exception {
        // Arrange: Empty task list
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "No tasks found.",
            "You're all caught up!",
            Collections.emptyList()
        );

        // Act
        notifier.notify(summary);

        // Assert
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage message = greenMail.getReceivedMessages()[0];

        String subject = message.getSubject();
        assertTrue(subject.contains("0 task"), "Subject should show 0 tasks");

        String body = GreenMailUtil.getBody(message).replaceAll("=\\r?\\n", "").replace("=3D", "=");
        assertTrue(body.contains("No tasks found"), "Body should contain summary text");
        assertTrue(body.contains("You're all caught up!"), "Body should contain schedule text");
    }

    private Task createTask(
        String title,
        String description,
        Priority priority,
        LocalDateTime deadline,
        TaskStatus status,
        String originalId
    ) {
        return new Task(
            java.util.UUID.randomUUID().toString(),
            title,
            description,
            deadline,
            priority,
            "Gmail",
            originalId,
            status,
            null,  // estimatedHours
            Collections.emptyList(),  // tags
            LocalDateTime.now(),  // createdAt
            LocalDateTime.now(),  // updatedAt
            ""  // notes
        );
    }
}
