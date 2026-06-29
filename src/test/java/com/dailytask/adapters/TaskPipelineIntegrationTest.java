package com.dailytask.adapters;

import com.dailytask.adapters.analyzers.SimpleTaskExtractor;
import com.dailytask.adapters.datasources.gmail.EmailToRawDataConverter;
import com.dailytask.core.domain.GmailMessage;
import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskPipelineIntegrationTest {

    private EmailToRawDataConverter rawDataConverter;
    private SimpleTaskExtractor taskExtractor;

    @BeforeEach
    void setUp() {
        rawDataConverter = new EmailToRawDataConverter();
        taskExtractor = new SimpleTaskExtractor();
    }

    @Test
    void testFullPipeline_emailWithClearDeadline() {
        GmailMessage gmailMessage = new GmailMessage();
        gmailMessage.setMessageId("msg-123");
        gmailMessage.setSubject("Assignment 5: Data Structures");
        gmailMessage.setFrom("sender@school.edu");
        gmailMessage.setBody("Complete the assignment. Due Friday");
        gmailMessage.setReceivedDate(LocalDateTime.of(2026, 6, 29, 10, 0));
        gmailMessage.setLabels(List.of("INBOX", "IMPORTANT"));

        RawData rawData = rawDataConverter.convert(gmailMessage);
        List<Task> tasks = taskExtractor.extract(List.of(rawData));

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertNotNull(task.getId());
        assertEquals("Assignment 5: Data Structures", task.getTitle());
        assertTrue(task.getDescription().contains("Complete the assignment"));
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-123", task.getOriginalId());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getDeadline());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    void testFullPipeline_emailWithMissingInfo() {
        GmailMessage gmailMessage = new GmailMessage();
        gmailMessage.setMessageId("msg-456");
        gmailMessage.setSubject("");
        gmailMessage.setFrom("unknown@example.com");
        gmailMessage.setBody("This is a simple task without much detail");
        gmailMessage.setReceivedDate(LocalDateTime.now());
        gmailMessage.setLabels(List.of("INBOX"));

        RawData rawData = rawDataConverter.convert(gmailMessage);
        List<Task> tasks = taskExtractor.extract(List.of(rawData));

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertNotNull(task.getId());
        assertNotNull(task.getTitle());
        assertFalse(task.getTitle().isEmpty());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNull(task.getDeadline());
    }

    @Test
    void testFullPipeline_malformedEmailBody() {
        GmailMessage gmailMessage = new GmailMessage();
        gmailMessage.setMessageId("msg-789");
        gmailMessage.setSubject("Malformed Task");
        gmailMessage.setFrom("sender@example.com");
        gmailMessage.setBody("Random text with no structure or deadline information xyz123!@#");
        gmailMessage.setReceivedDate(LocalDateTime.now());
        gmailMessage.setLabels(List.of("INBOX"));

        RawData rawData = rawDataConverter.convert(gmailMessage);
        List<Task> tasks = taskExtractor.extract(List.of(rawData));

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertNotNull(task.getId());
        assertEquals("Malformed Task", task.getTitle());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNull(task.getDeadline());
    }

    @Test
    void testFullPipeline_multipleEmails() {
        GmailMessage msg1 = new GmailMessage();
        msg1.setMessageId("msg-1");
        msg1.setSubject("Assignment 1");
        msg1.setFrom("prof@uni.edu");
        msg1.setBody("Due in 3 days");
        msg1.setReceivedDate(LocalDateTime.now());
        msg1.setLabels(List.of("INBOX"));

        GmailMessage msg2 = new GmailMessage();
        msg2.setMessageId("msg-2");
        msg2.setSubject("Project: Build App");
        msg2.setFrom("manager@company.com");
        msg2.setBody("Due May 25, 2026");
        msg2.setReceivedDate(LocalDateTime.now());
        msg2.setLabels(List.of("INBOX", "IMPORTANT"));

        List<RawData> rawDataList = List.of(
                rawDataConverter.convert(msg1),
                rawDataConverter.convert(msg2)
        );

        List<Task> tasks = taskExtractor.extract(rawDataList);

        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        Task task1 = tasks.get(0);
        assertEquals("Assignment 1", task1.getTitle());
        assertNotNull(task1.getDeadline());

        Task task2 = tasks.get(1);
        assertEquals("Project: Build App", task2.getTitle());
        assertNotNull(task2.getDeadline());
    }

    @Test
    void testFullPipeline_emailWithNumericDeadline() {
        GmailMessage gmailMessage = new GmailMessage();
        gmailMessage.setMessageId("msg-date");
        gmailMessage.setSubject("Numeric Date Task");
        gmailMessage.setFrom("sender@example.com");
        gmailMessage.setBody("Submit report. Due 7/15/2026");
        gmailMessage.setReceivedDate(LocalDateTime.now());
        gmailMessage.setLabels(List.of("INBOX"));

        RawData rawData = rawDataConverter.convert(gmailMessage);
        List<Task> tasks = taskExtractor.extract(List.of(rawData));

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertNotNull(task.getDeadline());
        assertEquals(7, task.getDeadline().getMonthValue());
        assertEquals(15, task.getDeadline().getDayOfMonth());
        assertEquals(2026, task.getDeadline().getYear());
    }

    @Test
    void testFullPipeline_emptyEmailList() {
        List<Task> tasks = taskExtractor.extract(List.of());

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }
}
