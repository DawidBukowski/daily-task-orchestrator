package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.RawData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleExtractor {

    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)assignment\\s+\\d+:\\s*[A-Za-z0-9 ]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROJECT_PATTERN = Pattern.compile(
            "(?i)project:\\s*[A-Za-z0-9 ]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUIZ_PATTERN = Pattern.compile(
            "(?i)quiz\\s+on\\s+[A-Za-z0-9 ]+",
            Pattern.CASE_INSENSITIVE
    );

    public String extractTitle(RawData rawData) {
        if (rawData == null) {
            return "Untitled Task";
        }

        String rawTitle = rawData.getTitle();
        if (rawTitle != null && !rawTitle.trim().isEmpty() && !isPlaceholder(rawTitle)) {
            return rawTitle.trim();
        }

        String content = rawData.getRawContent();
        if (content == null || content.trim().isEmpty()) {
            return "Untitled Task";
        }

        String extractedTitle = tryExtractPattern(content, ASSIGNMENT_PATTERN);
        if (extractedTitle != null) return extractedTitle;

        extractedTitle = tryExtractPattern(content, PROJECT_PATTERN);
        if (extractedTitle != null) return extractedTitle;

        extractedTitle = tryExtractPattern(content, QUIZ_PATTERN);
        if (extractedTitle != null) return extractedTitle;

        return extractFirstNCharacters(content, 50);
    }

    private boolean isPlaceholder(String title) {
        String lower = title.toLowerCase();
        return lower.contains("untitled") ||
               lower.contains("no subject") ||
               lower.contains("(no title)") ||
               lower.equals("n/a");
    }

    private String tryExtractPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String extractFirstNCharacters(String content, int maxLength) {
        String trimmed = content.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength).trim() + "...";
    }
}
