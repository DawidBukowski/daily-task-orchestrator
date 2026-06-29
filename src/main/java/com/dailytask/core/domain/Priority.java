package com.dailytask.core.domain;

public enum Priority {
    CRITICAL(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int numericValue;

    Priority(int numericValue) {
        this.numericValue = numericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }

    public static Priority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }

        String upperValue = value.trim().toUpperCase();
        try {
            return Priority.valueOf(upperValue);
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
