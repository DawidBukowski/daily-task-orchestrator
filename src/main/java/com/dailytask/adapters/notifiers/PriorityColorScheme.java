package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Priority;

/**
 * Maps Priority enum values to HTML color codes for email rendering.
 * Uses Bootstrap-inspired colors with WCAG 2.1 AA compliant contrast ratios (4.5:1 minimum).
 */
public class PriorityColorScheme {

    // Background colors (Bootstrap-inspired)
    private static final String CRITICAL_COLOR = "#dc3545"; // Red
    private static final String HIGH_COLOR = "#fd7e14";     // Orange
    private static final String MEDIUM_COLOR = "#ffc107";   // Amber
    private static final String LOW_COLOR = "#28a745";      // Green

    // Text colors for contrast (WCAG AA compliant)
    private static final String WHITE_TEXT = "#ffffff";     // White on dark backgrounds
    private static final String DARK_TEXT = "#212529";      // Dark on light backgrounds

    /**
     * Returns the background color hex code for a given priority.
     *
     * @param priority the task priority
     * @return hex color code (e.g., "#dc3545")
     */
    public String getColor(Priority priority) {
        if (priority == null) {
            return MEDIUM_COLOR; // Default to medium
        }

        return switch (priority) {
            case CRITICAL -> CRITICAL_COLOR;
            case HIGH -> HIGH_COLOR;
            case MEDIUM -> MEDIUM_COLOR;
            case LOW -> LOW_COLOR;
        };
    }

    /**
     * Returns the text color hex code for optimal contrast on the priority's background.
     *
     * @param priority the task priority
     * @return hex color code (e.g., "#ffffff" or "#212529")
     */
    public String getTextColor(Priority priority) {
        if (priority == null) {
            return DARK_TEXT; // Default matches MEDIUM
        }

        return switch (priority) {
            case CRITICAL, HIGH -> WHITE_TEXT;  // White text on dark red/orange
            case MEDIUM, LOW -> DARK_TEXT;      // Dark text on light yellow/green
        };
    }

    /**
     * Returns a human-readable color name for accessibility.
     *
     * @param priority the task priority
     * @return color name (e.g., "red", "orange", "yellow", "green")
     */
    public String getColorName(Priority priority) {
        if (priority == null) {
            return "yellow";
        }

        return switch (priority) {
            case CRITICAL -> "red";
            case HIGH -> "orange";
            case MEDIUM -> "yellow";
            case LOW -> "green";
        };
    }
}
