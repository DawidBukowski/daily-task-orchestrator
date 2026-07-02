package com.dailytask.core.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void testTaskCreation() {
        LocalDateTime now = LocalDateTime.now();
        List<String> tags = Arrays.asList("urgent", "homework");

        Task task = new Task(
                "123",
                "Complete Assignment",
                "Math homework",
                now.plusDays(2),
                Priority.HIGH,
                "Gmail",
                "msg-456",
                TaskStatus.PENDING,
                3.5,
                tags,
                now,
                now,
                "Important task"
        );

        assertEquals("123", task.getId());
        assertEquals("Complete Assignment", task.getTitle());
        assertEquals("Math homework", task.getDescription());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-456", task.getOriginalId());
        assertEquals(3.5, task.getEstimatedHours());
        assertEquals(2, task.getTags().size());
        assertTrue(task.getTags().contains("urgent"));
    }

    @Test
    void testIsOverdue_whenDeadlineInPast() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusDays(1);
        Task task = new Task(
                "1", "Test", "Description", pastDeadline,
                Priority.MEDIUM, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertTrue(task.isOverdue());
    }

    @Test
    void testIsOverdue_whenDeadlineInFuture() {
        LocalDateTime futureDeadline = LocalDateTime.now().plusDays(1);
        Task task = new Task(
                "1", "Test", "Description", futureDeadline,
                Priority.MEDIUM, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertFalse(task.isOverdue());
    }

    @Test
    void testIsOverdue_whenDeadlineIsNull() {
        Task task = new Task(
                "1", "Test", "Description", null,
                Priority.MEDIUM, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertFalse(task.isOverdue());
    }

    @Test
    void testDaysUntilDue_whenDeadlineInFuture() {
        LocalDateTime futureDeadline = LocalDateTime.now().plusDays(5);
        Task task = new Task(
                "1", "Test", "Description", futureDeadline,
                Priority.MEDIUM, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        long days = task.daysUntilDue();
        assertTrue(days >= 4 && days <= 5);
    }

    @Test
    void testDaysUntilDue_whenDeadlineIsNull() {
        Task task = new Task(
                "1", "Test", "Description", null,
                Priority.MEDIUM, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertEquals(0, task.daysUntilDue());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Task task1 = new Task(
                "1", "Test", "Description", now.plusDays(1),
                Priority.HIGH, "Source", "orig-1", TaskStatus.PENDING,
                null, null, now, now, null
        );

        Task task2 = new Task(
                "1", "Test", "Description", now.plusDays(1),
                Priority.HIGH, "Source", "orig-1", TaskStatus.PENDING,
                null, null, now, now, null
        );

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    @Test
    void testSettersAndGetters() {
        Task task = new Task(
                "1", "Test", "Description", LocalDateTime.now(),
                Priority.LOW, "Source", "orig-1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        task.setTitle("New Title");
        assertEquals("New Title", task.getTitle());

        task.setPriority(Priority.CRITICAL);
        assertEquals(Priority.CRITICAL, task.getPriority());

        task.setStatus(TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());

        task.setEstimatedHours(5.0);
        assertEquals(5.0, task.getEstimatedHours());

        task.setNotes("New notes");
        assertEquals("New notes", task.getNotes());
    }

    @Test
    void getGmailLink_withOriginalId_returnsGmailUrl() {
        Task task = new Task(
                "1", "Test", "Description", LocalDateTime.now(),
                Priority.MEDIUM, "Gmail", "18f3c4a5b2d8e9f1", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        String gmailLink = task.getGmailLink();
        assertEquals("https://mail.google.com/mail/u/0/#inbox/18f3c4a5b2d8e9f1", gmailLink);
    }

    @Test
    void getGmailLink_withNullOriginalId_returnsNull() {
        Task task = new Task(
                "1", "Test", "Description", LocalDateTime.now(),
                Priority.MEDIUM, "Manual", null, TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertNull(task.getGmailLink());
    }

    @Test
    void getGmailLink_withBlankOriginalId_returnsNull() {
        Task task = new Task(
                "1", "Test", "Description", LocalDateTime.now(),
                Priority.MEDIUM, "Manual", "   ", TaskStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertNull(task.getGmailLink());
    }
}
