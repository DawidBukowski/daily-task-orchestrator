package com.dailytask.adapters.analyzers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaudeResponseParser.
 *
 * Tests verify:
 * - Valid JSON parsing into structured response
 * - Robust error handling for malformed JSON
 * - Default values for missing fields
 * - Priority validation and normalization
 * - Task update validation
 */
class ClaudeResponseParserTest {

    private ClaudeResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ClaudeResponseParser();
    }

    @Test
    void constructor_withNullObjectMapper_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaudeResponseParser(null)
        );
    }

    @Test
    void parse_withNullJson_shouldReturnFallback() {
        ClaudeTaskSummaryResponse response = parser.parse(null);

        assertNotNull(response);
        assertEquals("No summary available", response.summary());
        assertEquals("No schedule available", response.schedule());
        assertTrue(response.recommendations().isEmpty());
        assertTrue(response.taskUpdates().isEmpty());
    }

    @Test
    void parse_withEmptyJson_shouldReturnFallback() {
        ClaudeTaskSummaryResponse response = parser.parse("");

        assertNotNull(response);
        assertEquals("No summary available", response.summary());
    }

    @Test
    void parse_withBlankJson_shouldReturnFallback() {
        ClaudeTaskSummaryResponse response = parser.parse("   \n\t  ");

        assertNotNull(response);
        assertEquals("No summary available", response.summary());
    }

    @Test
    void parse_withValidCompleteJson_shouldParseAllFields() {
        String json = """
            {
              "summary": "You have 3 high-priority tasks",
              "schedule": "Focus on critical items first",
              "recommendations": [
                "Break down large tasks",
                "Set realistic deadlines"
              ],
              "taskUpdates": [
                {
                  "taskId": "task-123",
                  "priority": "CRITICAL",
                  "estimatedHours": 5.5,
                  "notes": "Urgent deadline"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals("You have 3 high-priority tasks", response.summary());
        assertEquals("Focus on critical items first", response.schedule());
        assertEquals(2, response.recommendations().size());
        assertTrue(response.recommendations().contains("Break down large tasks"));
        assertEquals(1, response.taskUpdates().size());

        ClaudeTaskSummaryResponse.TaskUpdate update = response.taskUpdates().get(0);
        assertEquals("task-123", update.taskId());
        assertEquals("CRITICAL", update.priority());
        assertEquals(5.5, update.estimatedHours());
        assertEquals("Urgent deadline", update.notes());
    }

    @Test
    void parse_withMissingFields_shouldUseDefaults() {
        String json = """
            {
              "summary": "Brief summary"
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals("Brief summary", response.summary());
        assertEquals("No schedule", response.schedule());
        assertTrue(response.recommendations().isEmpty());
        assertTrue(response.taskUpdates().isEmpty());
    }

    @Test
    void parse_withEmptyArrays_shouldParseCorrectly() {
        String json = """
            {
              "summary": "All good",
              "schedule": "Continue as planned",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals("All good", response.summary());
        assertTrue(response.recommendations().isEmpty());
        assertTrue(response.taskUpdates().isEmpty());
    }

    @Test
    void parse_withInvalidPriority_shouldNormalizeToMedium() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "INVALID_PRIORITY",
                  "estimatedHours": 2.0,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals(1, response.taskUpdates().size());
        assertEquals("MEDIUM", response.taskUpdates().get(0).priority());
    }

    @Test
    void parse_withLowercasePriority_shouldNormalizeToUppercase() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "high",
                  "estimatedHours": 3.0,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals("HIGH", response.taskUpdates().get(0).priority());
    }

    @Test
    void parse_withMissingTaskId_shouldSkipUpdate() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "priority": "HIGH",
                  "estimatedHours": 2.0,
                  "notes": "Missing taskId"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertTrue(response.taskUpdates().isEmpty(), "Update with missing taskId should be skipped");
    }

    @Test
    void parse_withBlankTaskId_shouldSkipUpdate() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "  ",
                  "priority": "HIGH",
                  "estimatedHours": 2.0,
                  "notes": "Blank taskId"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertTrue(response.taskUpdates().isEmpty(), "Update with blank taskId should be skipped");
    }

    @Test
    void parse_withNegativeEstimatedHours_shouldIgnoreEstimate() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": -2.5,
                  "notes": "Negative hours"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(1, response.taskUpdates().size());
        assertNull(response.taskUpdates().get(0).estimatedHours(),
                "Negative hours should be ignored");
    }

    @Test
    void parse_withZeroEstimatedHours_shouldIgnoreEstimate() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 0.0,
                  "notes": "Zero hours"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNull(response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_withMissingEstimatedHours_shouldAllowNull() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "notes": "No hours specified"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(1, response.taskUpdates().size());
        assertNull(response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_withMultipleTaskUpdates_shouldParseAll() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 4.0,
                  "notes": "First"
                },
                {
                  "taskId": "task-2",
                  "priority": "LOW",
                  "estimatedHours": 1.5,
                  "notes": "Second"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(2, response.taskUpdates().size());
        assertEquals("task-1", response.taskUpdates().get(0).taskId());
        assertEquals("task-2", response.taskUpdates().get(1).taskId());
    }

    @Test
    void parse_withMalformedJson_shouldReturnFallback() {
        String malformedJson = "{ this is not valid json }";

        ClaudeTaskSummaryResponse response = parser.parse(malformedJson);

        assertNotNull(response);
        assertEquals("No summary available", response.summary());
    }

    @Test
    void parse_withUnknownFields_shouldIgnoreThem() {
        String json = """
            {
              "summary": "Test summary",
              "schedule": "Test schedule",
              "recommendations": ["Test"],
              "taskUpdates": [],
              "unknownField1": "ignored",
              "unknownField2": 123
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals("Test summary", response.summary());
    }

    @Test
    void parse_withEmptyRecommendations_shouldFilterOut() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": ["Valid", "", "  ", "Another valid"],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(2, response.recommendations().size());
        assertTrue(response.recommendations().contains("Valid"));
        assertTrue(response.recommendations().contains("Another valid"));
        assertFalse(response.recommendations().contains(""));
    }

    @Test
    void parse_withPartiallyInvalidTaskUpdates_shouldParseValidOnes() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 3.0,
                  "notes": "Valid"
                },
                {
                  "priority": "LOW",
                  "estimatedHours": 2.0,
                  "notes": "Missing taskId - invalid"
                },
                {
                  "taskId": "task-2",
                  "priority": "MEDIUM",
                  "estimatedHours": 1.0,
                  "notes": "Also valid"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(2, response.taskUpdates().size());
        assertEquals("task-1", response.taskUpdates().get(0).taskId());
        assertEquals("task-2", response.taskUpdates().get(1).taskId());
    }

    @Test
    void parse_withCustomObjectMapper_shouldUseIt() {
        ObjectMapper customMapper = new ObjectMapper();
        ClaudeResponseParser customParser = new ClaudeResponseParser(customMapper);

        String json = """
            {
              "summary": "Custom mapper test",
              "schedule": "Test schedule",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = customParser.parse(json);

        assertEquals("Custom mapper test", response.summary());
    }

    // ============ Enhanced Validation Tests ============

    @Test
    void parse_shouldNeverReturnNull() {
        String json = """
            {
              "summary": "Test"
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response, "Response should never be null");
        assertNotNull(response.summary(), "Summary should not be null");
        assertNotNull(response.schedule(), "Schedule should not be null");
        assertNotNull(response.recommendations(), "Recommendations should not be null");
        assertNotNull(response.taskUpdates(), "Task updates should not be null");
    }

    @Test
    void parse_withNullSummary_shouldUseDefault() {
        String json = """
            {
              "summary": null,
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response.summary());
        assertFalse(response.summary().isEmpty());
    }

    @Test
    void parse_withBlankSummary_shouldUseDefault() {
        String json = """
            {
              "summary": "   ",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertFalse(response.summary().contains("   "));
    }

    @Test
    void parse_withNullSchedule_shouldUseDefault() {
        String json = """
            {
              "summary": "Test",
              "schedule": null,
              "recommendations": [],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response.schedule());
        assertFalse(response.schedule().isEmpty());
    }

    @Test
    void parse_withAllPriorityLevels_shouldNormalizeCorrectly() {
        String[] priorities = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};

        for (String priority : priorities) {
            String json = """
                {
                  "summary": "Test",
                  "schedule": "Test",
                  "recommendations": [],
                  "taskUpdates": [
                    {
                      "taskId": "task-1",
                      "priority": "%s",
                      "estimatedHours": 1.0,
                      "notes": "Test"
                    }
                  ]
                }
                """.formatted(priority);

            ClaudeTaskSummaryResponse response = parser.parse(json);

            assertEquals(1, response.taskUpdates().size());
            assertEquals(priority, response.taskUpdates().get(0).priority());
        }
    }

    @Test
    void parse_withMixedCasePriorities_shouldNormalizeToUppercase() {
        String[] mixedCases = {"CrItIcAl", "HiGh", "MeDiUm", "lOw"};
        String[] expected = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};

        for (int i = 0; i < mixedCases.length; i++) {
            String json = """
                {
                  "summary": "Test",
                  "schedule": "Test",
                  "recommendations": [],
                  "taskUpdates": [
                    {
                      "taskId": "task-1",
                      "priority": "%s",
                      "estimatedHours": 1.0,
                      "notes": "Test"
                    }
                  ]
                }
                """.formatted(mixedCases[i]);

            ClaudeTaskSummaryResponse response = parser.parse(json);

            assertEquals(expected[i], response.taskUpdates().get(0).priority());
        }
    }

    @Test
    void parse_withValidPositiveDecimalHours_shouldAccept() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 2.75,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(2.75, response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_withLargeEstimatedHours_shouldAccept() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 999.5,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(999.5, response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_withVerySmallPositiveHours_shouldAccept() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 0.1,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(0.1, response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_withOptionalNotesField_shouldAllowNull() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 2.0
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(1, response.taskUpdates().size());
        // Notes should be empty string or null (implementation detail)
    }

    @Test
    void parse_withRecommendationsThatAreNotStrings_shouldSkipThem() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [
                "Valid string",
                123,
                null,
                true,
                "Another valid"
              ],
              "taskUpdates": []
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        // Should only include string recommendations
        assertEquals(2, response.recommendations().size());
        assertTrue(response.recommendations().contains("Valid string"));
        assertTrue(response.recommendations().contains("Another valid"));
    }

    @Test
    void parse_taskUpdateWithOnlyTaskIdAndPriority_shouldAccept() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(1, response.taskUpdates().size());
        assertEquals("task-1", response.taskUpdates().get(0).taskId());
        assertEquals("CRITICAL", response.taskUpdates().get(0).priority());
        assertNull(response.taskUpdates().get(0).estimatedHours());
    }

    @Test
    void parse_shouldFilterOutInvalidTaskUpdatesButKeepValid() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                {
                  "taskId": "valid-1",
                  "priority": "HIGH",
                  "estimatedHours": 2.0,
                  "notes": "Valid"
                },
                {
                  "taskId": "",
                  "priority": "HIGH",
                  "estimatedHours": 2.0,
                  "notes": "Empty taskId"
                },
                {
                  "taskId": "valid-2",
                  "priority": "MEDIUM",
                  "estimatedHours": 1.5,
                  "notes": "Also valid"
                },
                {
                  "priority": "LOW",
                  "estimatedHours": 1.0,
                  "notes": "Missing taskId"
                },
                {
                  "taskId": "valid-3",
                  "priority": "LOW",
                  "estimatedHours": 0.5,
                  "notes": "Valid again"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(3, response.taskUpdates().size(), "Should have 3 valid updates");
        assertEquals("valid-1", response.taskUpdates().get(0).taskId());
        assertEquals("valid-2", response.taskUpdates().get(1).taskId());
        assertEquals("valid-3", response.taskUpdates().get(2).taskId());
    }

    @Test
    void parse_witHugeLongResponseJson_shouldParseWithoutStackOverflow() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [
            """);

        // Add many recommendations
        for (int i = 0; i < 1000; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"Recommendation ").append(i).append("\"");
        }

        sb.append("""
                ],
              "taskUpdates": []
            }
            """);

        ClaudeTaskSummaryResponse response = parser.parse(sb.toString());

        assertNotNull(response);
        assertEquals(1000, response.recommendations().size());
    }

    @Test
    void parse_shouldPreserveTaskUpdateOrdering() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": [],
              "taskUpdates": [
                { "taskId": "z-first", "priority": "CRITICAL", "notes": "1st" },
                { "taskId": "a-second", "priority": "HIGH", "notes": "2nd" },
                { "taskId": "m-third", "priority": "MEDIUM", "notes": "3rd" },
                { "taskId": "b-fourth", "priority": "LOW", "notes": "4th" }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertEquals(4, response.taskUpdates().size());
        assertEquals("z-first", response.taskUpdates().get(0).taskId());
        assertEquals("a-second", response.taskUpdates().get(1).taskId());
        assertEquals("m-third", response.taskUpdates().get(2).taskId());
        assertEquals("b-fourth", response.taskUpdates().get(3).taskId());
    }

    @Test
    void parse_shouldReturnImmutableCollections() {
        String json = """
            {
              "summary": "Test",
              "schedule": "Test",
              "recommendations": ["Rec1"],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "HIGH",
                  "estimatedHours": 1.0,
                  "notes": "Test"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        // Collections should be immutable (attempting to modify should throw)
        assertThrows(UnsupportedOperationException.class, () ->
            response.recommendations().add("New recommendation"));

        assertThrows(UnsupportedOperationException.class, () ->
            response.taskUpdates().add(new ClaudeTaskSummaryResponse.TaskUpdate("task-2", "MEDIUM", null, "")));
    }
}
