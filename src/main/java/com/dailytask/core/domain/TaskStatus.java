package com.dailytask.core.domain;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static TaskStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }

        String upperValue = value.trim().toUpperCase();
        try {
            return TaskStatus.valueOf(upperValue);
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
