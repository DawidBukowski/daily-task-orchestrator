package com.dailytask.adapters.analyzers;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeadlineParser {

    private static final Pattern DUE_DAY_NAME_PATTERN = Pattern.compile(
            "(?i)due\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DUE_NUMERIC_DATE_PATTERN = Pattern.compile(
            "(?i)due\\s+(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DUE_MONTH_DAY_YEAR_PATTERN = Pattern.compile(
            "(?i)due\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2}),?\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DUE_IN_DAYS_PATTERN = Pattern.compile(
            "(?i)due\\s+in\\s+(\\d+)\\s+days?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FINAL_DAY_TIME_PATTERN = Pattern.compile(
            "(?i)final:\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday),\\s*(\\d{1,2})\\s*(am|pm)",
            Pattern.CASE_INSENSITIVE
    );

    public LocalDateTime extractDeadline(String text, LocalDateTime referenceTime) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        LocalDateTime result;

        result = parseDueDayName(text, referenceTime);
        if (result != null) return result;

        result = parseDueNumericDate(text, referenceTime);
        if (result != null) return result;

        result = parseDueMonthDayYear(text, referenceTime);
        if (result != null) return result;

        result = parseDueInDays(text, referenceTime);
        if (result != null) return result;

        result = parseFinalDayTime(text, referenceTime);
        if (result != null) return result;

        return null;
    }

    private LocalDateTime parseDueDayName(String text, LocalDateTime referenceTime) {
        Matcher matcher = DUE_DAY_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String dayName = matcher.group(1);
            DayOfWeek targetDay = parseDayOfWeek(dayName);
            if (targetDay != null) {
                LocalDateTime nextDay = referenceTime.with(TemporalAdjusters.next(targetDay));
                return nextDay.withHour(23).withMinute(59).withSecond(0).withNano(0);
            }
        }
        return null;
    }

    private LocalDateTime parseDueNumericDate(String text, LocalDateTime referenceTime) {
        Matcher matcher = DUE_NUMERIC_DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));
                String yearStr = matcher.group(3);
                int year = referenceTime.getYear();
                if (yearStr != null) {
                    year = Integer.parseInt(yearStr);
                    if (year < 100) {
                        year += 2000;
                    }
                }
                return LocalDateTime.of(year, month, day, 23, 59, 0);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime parseDueMonthDayYear(String text, LocalDateTime referenceTime) {
        Matcher matcher = DUE_MONTH_DAY_YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String monthName = matcher.group(1);
                int day = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                int month = parseMonthName(monthName);
                if (month > 0) {
                    return LocalDateTime.of(year, month, day, 23, 59, 0);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime parseDueInDays(String text, LocalDateTime referenceTime) {
        Matcher matcher = DUE_IN_DAYS_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int days = Integer.parseInt(matcher.group(1));
                return referenceTime.plusDays(days).withHour(23).withMinute(59).withSecond(0).withNano(0);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime parseFinalDayTime(String text, LocalDateTime referenceTime) {
        Matcher matcher = FINAL_DAY_TIME_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String dayName = matcher.group(1);
                int hour = Integer.parseInt(matcher.group(2));
                String amPm = matcher.group(3).toLowerCase();

                if (amPm.equals("pm") && hour != 12) {
                    hour += 12;
                } else if (amPm.equals("am") && hour == 12) {
                    hour = 0;
                }

                DayOfWeek targetDay = parseDayOfWeek(dayName);
                if (targetDay != null) {
                    LocalDateTime nextDay = referenceTime.with(TemporalAdjusters.next(targetDay));
                    return nextDay.withHour(hour).withMinute(0).withSecond(0).withNano(0);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private DayOfWeek parseDayOfWeek(String dayName) {
        if (dayName == null) return null;
        switch (dayName.toLowerCase()) {
            case "monday": return DayOfWeek.MONDAY;
            case "tuesday": return DayOfWeek.TUESDAY;
            case "wednesday": return DayOfWeek.WEDNESDAY;
            case "thursday": return DayOfWeek.THURSDAY;
            case "friday": return DayOfWeek.FRIDAY;
            case "saturday": return DayOfWeek.SATURDAY;
            case "sunday": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private int parseMonthName(String monthName) {
        if (monthName == null) return -1;
        switch (monthName.toLowerCase()) {
            case "january": return 1;
            case "february": return 2;
            case "march": return 3;
            case "april": return 4;
            case "may": return 5;
            case "june": return 6;
            case "july": return 7;
            case "august": return 8;
            case "september": return 9;
            case "october": return 10;
            case "november": return 11;
            case "december": return 12;
            default: return -1;
        }
    }
}
