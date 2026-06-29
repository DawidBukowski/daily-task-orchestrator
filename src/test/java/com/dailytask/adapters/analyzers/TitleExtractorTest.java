package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.RawData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TitleExtractorTest {

    private TitleExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TitleExtractor();
    }

    @Test
    void testExtractTitle_fromRawDataTitle() {
        RawData rawData = new RawData(
                "Gmail",
                "Assignment 5: Data Structures",
                "Body content",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "HIGH",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertEquals("Assignment 5: Data Structures", title);
    }

    @Test
    void testExtractTitle_assignmentPattern() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "Assignment 3: Algorithms and complexity analysis",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "HIGH",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.contains("Assignment 3"));
    }

    @Test
    void testExtractTitle_projectPattern() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "Project: Build a web application",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "MEDIUM",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.contains("Project"));
    }

    @Test
    void testExtractTitle_quizPattern() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "Quiz on Java fundamentals next week",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "MEDIUM",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.contains("Quiz"));
    }

    @Test
    void testExtractTitle_fallbackToFirst50Characters() {
        String longContent = "This is a very long piece of text that does not match any specific pattern but should be truncated";
        RawData rawData = new RawData(
                "Gmail",
                "",
                longContent,
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "LOW",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.length() <= 53); // 50 + "..."
        assertTrue(title.endsWith("..."));
    }

    @Test
    void testExtractTitle_shortContent() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "Short task",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "LOW",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertEquals("Short task", title);
    }

    @Test
    void testExtractTitle_nullRawData() {
        String title = extractor.extractTitle(null);
        assertEquals("Untitled Task", title);
    }

    @Test
    void testExtractTitle_emptyContent() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "LOW",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertEquals("Untitled Task", title);
    }

    @Test
    void testExtractTitle_placeholderTitle() {
        RawData rawData = new RawData(
                "Gmail",
                "(no title)",
                "Assignment 1: Complete homework",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "HIGH",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.contains("Assignment 1"));
    }

    @Test
    void testExtractTitle_caseInsensitive() {
        RawData rawData = new RawData(
                "Gmail",
                "",
                "ASSIGNMENT 10: Final Project",
                LocalDateTime.now(),
                "sender@example.com",
                "msg-123",
                "HIGH",
                Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.contains("ASSIGNMENT 10"));
    }
}
