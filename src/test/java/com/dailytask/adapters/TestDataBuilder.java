package com.dailytask.adapters;

import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.RawTask;
import com.dailytask.core.domain.Task;

import java.time.LocalDateTime;
import java.util.List;

public class TestDataBuilder {

    public static RawTask buildRawTask() {
        return new RawTask("Test Source", "Test Email", "Hello, do this task", LocalDateTime.now());
    }

    public static Task buildTask() {
        return new Task("1", "Process Email", "Hello, do this task",
                LocalDateTime.now().plusDays(1), "HIGH", "Test Source", "TODO");
    }

    public static AnalyzedTasks buildAnalyzedTasks() {
        return new AnalyzedTasks(
                List.of(buildTask()),
                "Test Summary",
                "Test Schedule",
                List.of("Recommendation 1")
        );
    }
}