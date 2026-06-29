package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TaskFactoryTest {

    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
    }

    @Test
    void testCreateFromRawData_completeData() {
        RawData rawData = new RawData(
                "Gmail",
                "Assignment 1",
                "Complete homework. Due Friday",
                LocalDateTime.of(2026, 6, 29, 10, 0),
                "sender@example.com",
                "msg-123",
                "HIGH",
                Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("Assignment 1", task.getTitle());
        assertEquals("Complete homework. Due Friday", task.getDescription());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-123", task.getOriginalId());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
        assertNotNull(task.getDeadline());
    }

    @Test
    void testCreateFromRawData_withDeadlineExtraction() {
        RawData rawData = new RawData(
                "Gmail",
                "Math Homework",
                "Due in 3 days",
                LocalDateTime.now(),
                "teacher@school.edu",
                "msg-456",
                "MEDIUM",
                Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertNotNull(task.getDeadline());
        assertTrue(task.getDeadline().isAfter(LocalDateTime.now()));
    }

    @Test
    void testCreateFromRawData_withoutDeadline() {
        RawData rawData = new RawData(
                "Gmail",
                "Simple Task",
                "No deadline mentioned",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-789",
                "LOW",
                Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertNull(task.getDeadline());
    }

    @Test
    void testCreateFromRawData_priorityParsing() {
        RawData rawData1 = new RawData("Gmail", "Task", "Content", LocalDateTime.now(),
                "sender", "id1", "CRITICAL", Collections.emptyMap());
        Task task1 = factory.createFromRawData(rawData1);
        assertEquals(Priority.CRITICAL, task1.getPriority());

        RawData rawData2 = new RawData("Gmail", "Task", "Content", LocalDateTime.now(),
                "sender", "id2", "low", Collections.emptyMap());
        Task task2 = factory.createFromRawData(rawData2);
        assertEquals(Priority.LOW, task2.getPriority());

        RawData rawData3 = new RawData("Gmail", "Task", "Content", LocalDateTime.now(),
                "sender", "id3", null, Collections.emptyMap());
        Task task3 = factory.createFromRawData(rawData3);
        assertEquals(Priority.MEDIUM, task3.getPriority());
    }

    @Test
    void testCreateFromRawData_defaultValues() {
        RawData rawData = new RawData(
                "Gmail",
                "Test Task",
                "Content",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-999",
                null,
                Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertNull(task.getEstimatedHours());
        assertNotNull(task.getTags());
        assertTrue(task.getTags().isEmpty());
        assertEquals("", task.getNotes());
    }

    @Test
    void testCreateFromRawData_nullRawData() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createFromRawData(null);
        });
    }

    @Test
    void testCreateFromRawData_emptyTitle() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "Assignment 2: Complete project",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-111",
                "HIGH",
                Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertTrue(task.getTitle().contains("Assignment 2"));
    }

    @Test
    void testCreateFromRawData_uniqueIds() {
        RawData rawData = new RawData(
                "Gmail",
                "Task",
                "Content",
                LocalDateTime.now(),
                "sender",
                "msg",
                "MEDIUM",
                Collections.emptyMap()
        );

        Task task1 = factory.createFromRawData(rawData);
        Task task2 = factory.createFromRawData(rawData);

        assertNotEquals(task1.getId(), task2.getId());
    }
}
