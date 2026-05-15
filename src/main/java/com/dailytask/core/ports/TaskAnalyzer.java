package com.dailytask.core.ports;

import com.dailytask.core.domain.AnalyzedTasks;
import com.dailytask.core.domain.Task;
import java.util.List;

/**
 * Contract for analyzing and organizing normalized tasks.
 */
public interface TaskAnalyzer {
    AnalyzedTasks analyze(List<Task> tasks);
}