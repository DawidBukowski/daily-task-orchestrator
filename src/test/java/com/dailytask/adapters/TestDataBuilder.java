package com.dailytask.adapters;

import com.dailytask.core.domain.Priority;
import com.dailytask.core.domain.TasksSummary;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.domain.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public class TestDataBuilder {

    public static RawData buildRawData() {
        return new RawData("Test Source", "Test Email", "Hello, do this task", LocalDateTime.now());
    }

    public static Task buildData() {
        LocalDateTime now = LocalDateTime.now();
        return new Task(
                "1",
                "Process Email",
                "Hello, do this task",
                now.plusDays(1),
                Priority.HIGH,
                "Test Source",
                "orig-1",
                TaskStatus.PENDING,
                null,
                List.of(),
                now,
                now,
                ""
        );
    }

    public static TasksSummary buildSummarizedTasks() {
        return new TasksSummary(
                List.of(buildData()),
                "Test Summary",
                "Test Schedule",
                List.of("Recommendation 1")
        );
    }
}
