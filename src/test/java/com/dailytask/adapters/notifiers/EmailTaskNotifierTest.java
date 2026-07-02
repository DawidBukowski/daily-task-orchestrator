package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import com.dailytask.core.domain.TasksSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailTaskNotifierTest {

    private EmailConfiguration config;
    private EmailTemplate emailTemplate;
    private SmtpEmailSender emailSender;
    private EmailTaskNotifier notifier;

    @BeforeEach
    void setUp() {
        config = EmailConfiguration.builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(587)
            .username("test@example.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .build();

        emailTemplate = mock(EmailTemplate.class);
        emailSender = mock(SmtpEmailSender.class);
        notifier = new EmailTaskNotifier(config, emailTemplate, emailSender);
    }

    @Test
    void constructor_withNullConfig_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailTaskNotifier(null, emailTemplate, emailSender));
    }

    @Test
    void constructor_withNullTemplate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailTaskNotifier(config, null, emailSender));
    }

    @Test
    void constructor_withNullSender_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailTaskNotifier(config, emailTemplate, null));
    }

    @Test
    void notify_withNullSummary_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> notifier.notify(null));
    }

    @Test
    void notify_withValidSummary_generatesAndSendsEmail() throws Exception {
        Task task = createTask("Test Task", Priority.HIGH, LocalDateTime.now().plusDays(1));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        when(emailTemplate.generateSubject(summary)).thenReturn("Daily Task Summary: 1 task");
        when(emailTemplate.generateHtml(summary)).thenReturn("<html>Test HTML</html>");

        notifier.notify(summary);

        verify(emailTemplate).generateSubject(summary);
        verify(emailTemplate).generateHtml(summary);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(subjectCaptor.capture(), htmlCaptor.capture());

        assertEquals("Daily Task Summary: 1 task", subjectCaptor.getValue());
        assertEquals("<html>Test HTML</html>", htmlCaptor.getValue());
    }

    @Test
    void notify_whenSmtpSenderFails_logsErrorAndDoesNotThrow() throws Exception {
        Task task = createTask("Test Task", Priority.MEDIUM, LocalDateTime.now().plusDays(2));
        TasksSummary summary = new TasksSummary(
            Collections.singletonList(task),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        when(emailTemplate.generateSubject(summary)).thenReturn("Subject");
        when(emailTemplate.generateHtml(summary)).thenReturn("<html>Body</html>");
        doThrow(new SmtpEmailSender.EmailSendException("SMTP error", new RuntimeException()))
            .when(emailSender).send(anyString(), anyString());

        // Should not throw - graceful degradation
        assertDoesNotThrow(() -> notifier.notify(summary));

        verify(emailSender).send(anyString(), anyString());
    }

    @Test
    void notify_whenTemplateGenerationFails_logsErrorAndDoesNotThrow() {
        TasksSummary summary = new TasksSummary(
            Collections.emptyList(),
            "Summary",
            "Schedule",
            Collections.emptyList()
        );

        when(emailTemplate.generateSubject(summary)).thenThrow(new RuntimeException("Template error"));

        // Should not throw - graceful degradation
        assertDoesNotThrow(() -> notifier.notify(summary));

        verify(emailTemplate).generateSubject(summary);
        verifyNoInteractions(emailSender);
    }

    private Task createTask(String title, Priority priority, LocalDateTime deadline) {
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
