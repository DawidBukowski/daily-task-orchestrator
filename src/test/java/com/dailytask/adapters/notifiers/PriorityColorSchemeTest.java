package com.dailytask.adapters.notifiers;

import com.dailytask.core.domain.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityColorSchemeTest {

    private PriorityColorScheme colorScheme;

    @BeforeEach
    void setUp() {
        colorScheme = new PriorityColorScheme();
    }

    @Test
    void getColor_forCriticalPriority_returnsRed() {
        String color = colorScheme.getColor(Priority.CRITICAL);
        assertEquals("#dc3545", color);
    }

    @Test
    void getColor_forHighPriority_returnsOrange() {
        String color = colorScheme.getColor(Priority.HIGH);
        assertEquals("#fd7e14", color);
    }

    @Test
    void getColor_forMediumPriority_returnsAmber() {
        String color = colorScheme.getColor(Priority.MEDIUM);
        assertEquals("#ffc107", color);
    }

    @Test
    void getColor_forLowPriority_returnsGreen() {
        String color = colorScheme.getColor(Priority.LOW);
        assertEquals("#28a745", color);
    }

    @Test
    void getColor_forNullPriority_returnsMediumColor() {
        String color = colorScheme.getColor(null);
        assertEquals("#ffc107", color); // Same as MEDIUM
    }

    @Test
    void getTextColor_forCriticalPriority_returnsWhite() {
        String textColor = colorScheme.getTextColor(Priority.CRITICAL);
        assertEquals("#ffffff", textColor);
    }

    @Test
    void getTextColor_forHighPriority_returnsWhite() {
        String textColor = colorScheme.getTextColor(Priority.HIGH);
        assertEquals("#ffffff", textColor);
    }

    @Test
    void getTextColor_forMediumPriority_returnsDark() {
        String textColor = colorScheme.getTextColor(Priority.MEDIUM);
        assertEquals("#212529", textColor);
    }

    @Test
    void getTextColor_forLowPriority_returnsDark() {
        String textColor = colorScheme.getTextColor(Priority.LOW);
        assertEquals("#212529", textColor);
    }

    @Test
    void getTextColor_forNullPriority_returnsDark() {
        String textColor = colorScheme.getTextColor(null);
        assertEquals("#212529", textColor); // Same as MEDIUM
    }

    @Test
    void getColorName_forCriticalPriority_returnsRed() {
        String colorName = colorScheme.getColorName(Priority.CRITICAL);
        assertEquals("red", colorName);
    }

    @Test
    void getColorName_forHighPriority_returnsOrange() {
        String colorName = colorScheme.getColorName(Priority.HIGH);
        assertEquals("orange", colorName);
    }

    @Test
    void getColorName_forMediumPriority_returnsYellow() {
        String colorName = colorScheme.getColorName(Priority.MEDIUM);
        assertEquals("yellow", colorName);
    }

    @Test
    void getColorName_forLowPriority_returnsGreen() {
        String colorName = colorScheme.getColorName(Priority.LOW);
        assertEquals("green", colorName);
    }

    @Test
    void getColorName_forNullPriority_returnsYellow() {
        String colorName = colorScheme.getColorName(null);
        assertEquals("yellow", colorName); // Same as MEDIUM
    }

    @Test
    void allPriorityValues_haveColorMappings() {
        for (Priority priority : Priority.values()) {
            String color = colorScheme.getColor(priority);
            assertNotNull(color, "Color should not be null for " + priority);
            assertTrue(color.startsWith("#"), "Color should be hex code for " + priority);
            assertEquals(7, color.length(), "Color hex code should be 7 characters for " + priority);
        }
    }

    @Test
    void allPriorityValues_haveTextColorMappings() {
        for (Priority priority : Priority.values()) {
            String textColor = colorScheme.getTextColor(priority);
            assertNotNull(textColor, "Text color should not be null for " + priority);
            assertTrue(textColor.startsWith("#"), "Text color should be hex code for " + priority);
            assertEquals(7, textColor.length(), "Text color hex code should be 7 characters for " + priority);
        }
    }

    @Test
    void allPriorityValues_haveColorNameMappings() {
        for (Priority priority : Priority.values()) {
            String colorName = colorScheme.getColorName(priority);
            assertNotNull(colorName, "Color name should not be null for " + priority);
            assertFalse(colorName.isBlank(), "Color name should not be blank for " + priority);
        }
    }
}
