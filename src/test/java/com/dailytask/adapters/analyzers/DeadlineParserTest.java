package com.dailytask.adapters.analyzers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineParserTest {

    private DeadlineParser parser;
    private LocalDateTime referenceTime;

    @BeforeEach
    void setUp() {
        parser = new DeadlineParser();
        referenceTime = LocalDateTime.of(2026, 6, 29, 10, 0);
    }

    @Test
    void testParseDueFriday() {
        String text = "Due Friday";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertTrue(result.isAfter(referenceTime));
    }

    @Test
    void testParseDueNumericDate() {
        String text = "Due 7/5";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(7, result.getMonthValue());
        assertEquals(5, result.getDayOfMonth());
        assertEquals(2026, result.getYear());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
    }

    @Test
    void testParseDueNumericDateWithYear() {
        String text = "Due 5/25/2026";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(5, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(2026, result.getYear());
    }

    @Test
    void testParseDueMonthDayYear() {
        String text = "Due May 25, 2026";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(5, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(2026, result.getYear());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
    }

    @Test
    void testParseDueInDays() {
        String text = "Due in 3 days";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(referenceTime.plusDays(3).toLocalDate(), result.toLocalDate());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
    }

    @Test
    void testParseFinalDayTime() {
        String text = "Final: Friday, 2 PM";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        assertEquals(14, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    void testParseFinalDayTimeAM() {
        String text = "Final: Monday, 9 AM";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
        assertEquals(9, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    void testParseNullText() {
        LocalDateTime result = parser.extractDeadline(null, referenceTime);
        assertNull(result);
    }

    @Test
    void testParseEmptyText() {
        LocalDateTime result = parser.extractDeadline("", referenceTime);
        assertNull(result);
    }

    @Test
    void testParseUnmatchedText() {
        String text = "No deadline information here";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);
        assertNull(result);
    }

    @Test
    void testCaseInsensitive() {
        String text = "DUE FRIDAY";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
    }

    @Test
    void testDueInOneDay() {
        String text = "Due in 1 day";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(referenceTime.plusDays(1).toLocalDate(), result.toLocalDate());
    }
}
