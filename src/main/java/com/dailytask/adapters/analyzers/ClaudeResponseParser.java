package com.dailytask.adapters.analyzers;

import com.dailytask.core.domain.Priority;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Claude API JSON responses into typed domain objects.
 *
 * <p>This parser implements robust error handling with graceful degradation:
 * <ul>
 *   <li>Unknown JSON fields are ignored (forward compatibility)</li>
 *   <li>Missing required fields use safe defaults</li>
 *   <li>Invalid priority values fall back to MEDIUM</li>
 *   <li>Malformed JSON returns a fallback response</li>
 * </ul>
 *
 * <p>The parser treats Claude's response as an external API contract that must
 * be validated and sanitized before entering the domain layer.
 */
public class ClaudeResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeResponseParser.class);
    private final ObjectMapper objectMapper;

    /**
     * Creates a new parser with default Jackson configuration.
     *
     * <p>The ObjectMapper is configured to ignore unknown properties,
     * ensuring forward compatibility with schema changes.
     */
    public ClaudeResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new parser with a custom ObjectMapper.
     *
     * @param objectMapper the Jackson ObjectMapper to use for parsing
     * @throws IllegalArgumentException if objectMapper is null
     */
    public ClaudeResponseParser(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper must not be null");
        }
        this.objectMapper = objectMapper;
    }

    /**
     * Parses Claude's JSON response into a structured response object.
     *
     * <p>This method implements a multi-stage parsing strategy:
     * <ol>
     *   <li>Strip markdown code fences (if present)</li>
     *   <li>Parse raw JSON into tree structure</li>
     *   <li>Extract and validate top-level fields</li>
     *   <li>Parse task updates array with per-item validation</li>
     *   <li>Apply defaults for missing or invalid data</li>
     * </ol>
     *
     * <p>Any parsing errors or validation failures result in a fallback
     * response that won't disrupt the application flow.
     *
     * @param jsonResponse the raw JSON string from Claude API (must not be null)
     * @return a parsed response with validated data, or a fallback on error
     * @throws IllegalArgumentException if jsonResponse is null
     */
    public ClaudeTaskSummaryResponse parse(String jsonResponse) {
        if (jsonResponse == null) {
            logger.warn("Received null JSON response, returning fallback");
            return ClaudeTaskSummaryResponse.createFallback();
        }

        if (jsonResponse.isBlank()) {
            logger.warn("Received empty JSON response, returning fallback");
            return ClaudeTaskSummaryResponse.createFallback();
        }

        try {
            // Strip markdown code fences if present (Claude sometimes wraps JSON in ```json ... ```)
            String cleanedJson = stripMarkdownCodeFences(jsonResponse);

            JsonNode rootNode = objectMapper.readTree(cleanedJson);

            // Extract top-level fields with null-safety
            String summary = extractTextField(rootNode, "summary", "No summary");
            String schedule = extractTextField(rootNode, "schedule", "No schedule");
            List<String> recommendations = extractRecommendations(rootNode);
            List<ClaudeTaskSummaryResponse.TaskUpdate> taskUpdates = extractTaskUpdates(rootNode);

            logger.debug("Successfully parsed response: {} recommendations, {} task updates",
                    recommendations.size(), taskUpdates.size());

            return ClaudeTaskSummaryResponse.createWithDefaults(
                    summary,
                    schedule,
                    recommendations,
                    taskUpdates
            );

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON response: {}", e.getMessage());
            logger.debug("Malformed JSON (first 200 chars): {}",
                    jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            return ClaudeTaskSummaryResponse.createFallback();
        } catch (Exception e) {
            logger.error("Unexpected error parsing response: {}", e.getMessage(), e);
            return ClaudeTaskSummaryResponse.createFallback();
        }
    }

    /**
     * Strips markdown code fences from response text.
     *
     * <p>Claude sometimes wraps JSON responses in markdown code blocks like:
     * <pre>
     * ```json
     * {"key": "value"}
     * ```
     * </pre>
     *
     * This method removes those fences, returning just the JSON content.
     *
     * @param response the raw response text (potentially with code fences)
     * @return the cleaned JSON string without markdown formatting
     */
    private String stripMarkdownCodeFences(String response) {
        String trimmed = response.trim();

        // Check if wrapped in code fences
        if (trimmed.startsWith("```")) {
            // Find the end of the opening fence (first newline after ```)
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline == -1) {
                // No newline found, return as-is
                return response;
            }

            // Check if response ends with closing ```
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence <= firstNewline) {
                // No proper closing fence, return as-is
                return response;
            }

            // Extract content between fences
            String content = trimmed.substring(firstNewline + 1, closingFence).trim();
            logger.debug("Stripped markdown code fences from response");
            return content;
        }

        return response;
    }

    /**
     * Extracts a text field from JSON with fallback default.
     *
     * @param node the JSON node containing the field
     * @param fieldName the name of the field to extract
     * @param defaultValue the default if field is missing or invalid
     * @return the extracted text or default value
     */
    private String extractTextField(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !fieldNode.isTextual()) {
            logger.debug("Field '{}' missing or not text, using default", fieldName);
            return defaultValue;
        }
        String value = fieldNode.asText();
        return value.isBlank() ? defaultValue : value;
    }

    /**
     * Extracts the recommendations array from JSON.
     *
     * @param rootNode the root JSON node
     * @return a list of recommendation strings, or empty list on error
     */
    private List<String> extractRecommendations(JsonNode rootNode) {
        JsonNode recommendationsNode = rootNode.get("recommendations");
        if (recommendationsNode == null || !recommendationsNode.isArray()) {
            logger.debug("recommendations field missing or not an array, using empty list");
            return List.of();
        }

        List<String> recommendations = new ArrayList<>();
        for (JsonNode item : recommendationsNode) {
            if (item.isTextual() && !item.asText().isBlank()) {
                recommendations.add(item.asText());
            }
        }

        return recommendations;
    }

    /**
     * Extracts and validates the taskUpdates array from JSON.
     *
     * <p>Each task update is validated for:
     * <ul>
     *   <li>taskId presence and non-blank value</li>
     *   <li>priority validity using {@link Priority#fromString}</li>
     *   <li>estimatedHours positivity</li>
     * </ul>
     *
     * Invalid updates are logged and skipped, not causing complete failure.
     *
     * @param rootNode the root JSON node
     * @return a list of validated task updates, or empty list if none valid
     */
    private List<ClaudeTaskSummaryResponse.TaskUpdate> extractTaskUpdates(JsonNode rootNode) {
        JsonNode updatesNode = rootNode.get("taskUpdates");
        if (updatesNode == null || !updatesNode.isArray()) {
            logger.debug("taskUpdates field missing or not an array, using empty list");
            return List.of();
        }

        List<ClaudeTaskSummaryResponse.TaskUpdate> taskUpdates = new ArrayList<>();

        for (int i = 0; i < updatesNode.size(); i++) {
            JsonNode updateNode = updatesNode.get(i);
            try {
                ClaudeTaskSummaryResponse.TaskUpdate update = parseTaskUpdate(updateNode, i);
                if (update != null) {
                    taskUpdates.add(update);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse task update at index {}: {}", i, e.getMessage());
            }
        }

        return taskUpdates;
    }

    /**
     * Parses a single task update with validation.
     *
     * @param updateNode the JSON node for one task update
     * @param index the array index (for logging)
     * @return a validated TaskUpdate, or null if invalid
     */
    private ClaudeTaskSummaryResponse.TaskUpdate parseTaskUpdate(JsonNode updateNode, int index) {
        if (updateNode == null || !updateNode.isObject()) {
            logger.warn("Task update at index {} is not an object", index);
            return null;
        }

        // Extract and validate taskId (critical field)
        JsonNode taskIdNode = updateNode.get("taskId");
        if (taskIdNode == null || !taskIdNode.isTextual() || taskIdNode.asText().isBlank()) {
            logger.warn("Task update at index {} missing or blank taskId", index);
            return null;
        }
        String taskId = taskIdNode.asText();

        // Extract and validate priority (with fallback to MEDIUM)
        String priorityString = extractTextField(updateNode, "priority", "MEDIUM");
        Priority priority = Priority.fromString(priorityString);
        if (!priorityString.equals(priority.name())) {
            logger.debug("Priority '{}' normalized to {} for task {}",
                    priorityString, priority, taskId);
        }

        // Extract and validate estimatedHours
        JsonNode hoursNode = updateNode.get("estimatedHours");
        Double estimatedHours = null;
        if (hoursNode != null && hoursNode.isNumber()) {
            double hours = hoursNode.asDouble();
            if (hours > 0) {
                estimatedHours = hours;
            } else {
                logger.warn("Task {} has non-positive estimatedHours: {}, ignoring", taskId, hours);
            }
        }

        // Extract notes (optional)
        String notes = extractTextField(updateNode, "notes", "");

        return new ClaudeTaskSummaryResponse.TaskUpdate(
                taskId,
                priority.name(),
                estimatedHours,
                notes
        );
    }
}
