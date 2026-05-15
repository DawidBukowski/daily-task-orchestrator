package com.dailytask.core.ports;

import com.dailytask.core.domain.AnalyzedTasks;

/**
 * Contract for dispatching the finalized task analysis to the user.
 */
public interface TaskNotifier {
    void notify(AnalyzedTasks tasks);
}