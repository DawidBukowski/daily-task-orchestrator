package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.ports.TaskExtractor;

import java.util.ArrayList;
import java.util.List;

public class SimpleTaskExtractor implements TaskExtractor {

    private final TaskFactory taskFactory;

    public SimpleTaskExtractor() {
        this.taskFactory = new TaskFactory();
    }

    @Override
    public List<Task> extract(List<RawData> rawDataList) {
        if (rawDataList == null || rawDataList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        for (RawData rawData : rawDataList) {
            try {
                Task task = taskFactory.createFromRawData(rawData);
                tasks.add(task);
            } catch (Exception e) {
                System.err.println("Failed to extract task from raw data: " + e.getMessage());
            }
        }

        return tasks;
    }
}
