package com.dailytask.adapters.analyzers;

import com.dailytask.core.config.ClaudeConfiguration;
import com.dailytask.core.ports.ClaudeApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Direct Anthropic API client implementation using Java 21 HttpClient.
 * Communicates directly with the Anthropic Messages API.
 */
public class DirectAnthropicClient implements ClaudeApiClient {
    private static final Logger logger = LoggerFactory.getLogger(DirectAnthropicClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ClaudeConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DirectAnthropicClient(ClaudeConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);
            logger.debug("Sending request to Anthropic API");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getAnthropicApiUrl()))
                .header("x-api-key", config.getAnthropicApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            return handleResponse(response);

        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("Request to Anthropic API timed out after {} seconds", config.getTimeoutSeconds());
            throw new ClaudeApiClient.ClaudeApiException(
                "Request timed out after " + config.getTimeoutSeconds() + " seconds",
                ClaudeApiClient.ClaudeApiException.ErrorType.TIMEOUT,
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Request interrupted", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Request was interrupted",
                ClaudeApiClient.ClaudeApiException.ErrorType.NETWORK_ERROR,
                e
            );
        } catch (ClaudeApiClient.ClaudeApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error calling Anthropic API", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Unexpected error: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.NETWORK_ERROR,
                e
            );
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", config.getModelId());
            root.put("max_tokens", config.getMaxTokens());
            root.put("temperature", config.getTemperature());

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                root.put("system", systemPrompt);
            }

            ArrayNode messages = root.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new ClaudeApiClient.ClaudeApiException(
                "Failed to build request body",
                ClaudeApiClient.ClaudeApiException.ErrorType.INVALID_REQUEST,
                e
            );
        }
    }

    private String handleResponse(HttpResponse<String> response) throws ClaudeApiClient.ClaudeApiException {
        int statusCode = response.statusCode();

        if (statusCode == 200) {
            return extractTextFromResponse(response.body());
        }

        // Handle error responses
        String errorMessage = extractErrorMessage(response.body(), statusCode);

        if (statusCode == 401 || statusCode == 403) {
            logger.error("Authentication failed: {}", errorMessage);
            throw new ClaudeApiClient.ClaudeApiException(
                "Authentication failed. Please check your API key.",
                ClaudeApiClient.ClaudeApiException.ErrorType.AUTHENTICATION_FAILED,
                statusCode
            );
        }

        if (statusCode == 429) {
            logger.warn("Rate limit exceeded: {}", errorMessage);
            throw new ClaudeApiClient.ClaudeApiException(
                "Rate limit exceeded. Please try again later.",
                ClaudeApiClient.ClaudeApiException.ErrorType.RATE_LIMIT_EXCEEDED,
                statusCode
            );
        }

        if (statusCode >= 500) {
            logger.error("Server error from Anthropic API: {}", errorMessage);
            throw new ClaudeApiClient.ClaudeApiException(
                "Anthropic API server error: " + errorMessage,
                ClaudeApiClient.ClaudeApiException.ErrorType.SERVER_ERROR,
                statusCode
            );
        }

        // Client errors (4xx)
        logger.error("Invalid request to Anthropic API: {}", errorMessage);
        throw new ClaudeApiClient.ClaudeApiException(
            "Invalid request: " + errorMessage,
            ClaudeApiClient.ClaudeApiException.ErrorType.INVALID_REQUEST,
            statusCode
        );
    }

    private String extractTextFromResponse(String responseBody) throws ClaudeApiClient.ClaudeApiException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.get("content");

            if (content == null || !content.isArray() || content.isEmpty()) {
                throw new ClaudeApiClient.ClaudeApiException(
                    "Response does not contain expected 'content' array",
                    ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE
                );
            }

            // Extract text from the first content block
            JsonNode firstBlock = content.get(0);
            JsonNode text = firstBlock.get("text");

            if (text == null || !text.isTextual()) {
                throw new ClaudeApiClient.ClaudeApiException(
                    "Content block does not contain expected 'text' field",
                    ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE
                );
            }

            String responseText = text.asText();
            logger.debug("Successfully extracted response text ({} characters)", responseText.length());
            return responseText;

        } catch (ClaudeApiClient.ClaudeApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse response body", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Failed to parse response: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE,
                e
            );
        }
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.get("error");
            if (error != null) {
                JsonNode message = error.get("message");
                if (message != null && message.isTextual()) {
                    return message.asText();
                }
            }
            return responseBody;
        } catch (Exception e) {
            logger.debug("Could not parse error response body", e);
            return "HTTP " + statusCode + " - Unable to parse error details";
        }
    }
}
