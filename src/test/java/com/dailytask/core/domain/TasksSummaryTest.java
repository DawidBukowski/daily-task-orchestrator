package com.dailytask.core.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TasksSummaryTest {

    @Test
    void testGetTasksSortedByPriority() {
        Task task1 = createTask("1", Priority.LOW, LocalDateTime.now().plusDays(1));
        Task task2 = createTask("2", Priority.CRITICAL, LocalDateTime.now().plusDays(2));
        Task task3 = createTask("3", Priority.MEDIUM, LocalDateTime.now().plusDays(3));
        Task task4 = createTask("4", Priority.HIGH, LocalDateTime.now().plusDays(4));

        TasksSummary summary = new TasksSummary(
                Arrays.asList(task1, task2, task3, task4),
                "Test Summary",
                "Test Schedule",
                List.of("Rec1")
        );

        List<Task> sorted = summary.getTasksSortedByPriority();

        assertEquals(4, sorted.size());
        assertEquals(Priority.CRITICAL, sorted.get(0).getPriority());
        assertEquals(Priority.HIGH, sorted.get(1).getPriority());
        assertEquals(Priority.MEDIUM, sorted.get(2).getPriority());
        assertEquals(Priority.LOW, sorted.get(3).getPriority());
    }

    @Test
    void testGetTodaysTasks() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

        Task task1 = createTask("1", Priority.HIGH, today);
        Task task2 = createTask("2", Priority.MEDIUM, tomorrow);
        Task task3 = createTask("3", Priority.LOW, today);

        TasksSummary summary = new TasksSummary(
                Arrays.asList(task1, task2, task3),
                "Test Summary",
                "Test Schedule",
                List.of("Rec1")
        );

        List<Task> todaysTasks = summary.getTodaysTasks();

        assertEquals(2, todaysTasks.size());
        assertTrue(todaysTasks.contains(task1));
        assertTrue(todaysTasks.contains(task3));
        assertFalse(todaysTasks.contains(task2));
    }

    @Test
    void testGetOverdueTasks() {
        LocalDateTime past = LocalDateTime.now().minusDays(2);
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        Task task1 = createTask("1", Priority.HIGH, past);
        Task task2 = createTask("2", Priority.MEDIUM, future);
        Task task3 = createTask("3", Priority.LOW, past);

        TasksSummary summary = new TasksSummary(
                Arrays.asList(task1, task2, task3),
                "Test Summary",
                "Test Schedule",
                List.of("Rec1")
        );

        List<Task> overdueTasks = summary.getOverdueTasks();

        assertEquals(2, overdueTasks.size());
        assertTrue(overdueTasks.contains(task1));
        assertTrue(overdueTasks.contains(task3));
        assertFalse(overdueTasks.contains(task2));
    }

    @Test
    void testGetSchedule() {
        TasksSummary summary = new TasksSummary(
                List.of(),
                "Test Summary",
                "Morning: 9-12, Afternoon: 2-5",
                List.of("Rec1")
        );

        assertEquals("Morning: 9-12, Afternoon: 2-5", summary.getSchedule());
    }

    @Test
    void testEmptyTaskList() {
        TasksSummary summary = new TasksSummary(
                null,
                "Empty Summary",
                "No Schedule",
                List.of()
        );

        assertEquals(0, summary.getTasksSortedByPriority().size());
        assertEquals(0, summary.getTodaysTasks().size());
        assertEquals(0, summary.getOverdueTasks().size());
    }

    private Task createTask(String id, Priority priority, LocalDateTime deadline) {
        return new Task(
                id,
                "Task " + id,
                "Description",
                deadline,
                priority,
                "Source",
                "orig-" + id,
                TaskStatus.PENDING,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }
}
