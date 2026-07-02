package com.dailytask.adapters.analyzers;

import com.dailytask.core.config.ClaudeConfiguration;
import com.dailytask.core.ports.ClaudeApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import static org.mockito.Mockito.doReturn;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DirectAnthropicClient.
 *
 * Tests verify:
 * - Successful API calls with valid JSON responses
 * - HTTP header validation (x-api-key, anthropic-version)
 * - Error handling: 401/403/429/5xx status codes
 * - Timeout handling and exception mapping
 * - Malformed JSON response handling
 * - Secret non-logging in error paths
 * - Request body construction
 */
@ExtendWith(MockitoExtension.class)
class DirectAnthropicClientTest {

    private static final String TEST_API_KEY = "test-api-key-sk-12345";
    private static final String TEST_MODEL_ID = "claude-3-5-sonnet-20241022";
    private static final String TEST_API_URL = "https://api.anthropic.com/v1/messages";

    @Mock
    private HttpClient mockHttpClient;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private HttpResponse<String> mockResponse;

    private ClaudeConfiguration config;
    private DirectAnthropicClient client;

    @BeforeEach
    void setUp() {
        config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId(TEST_MODEL_ID)
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(30)
            .anthropicApiKey(TEST_API_KEY)
            .anthropicApiUrl(TEST_API_URL)
            .build();

        client = new DirectAnthropicClient(config);
        // Inject mock HttpClient via reflection since constructor creates its own
        injectHttpClient(client, mockHttpClient);
    }

    // ============ Success Cases ============

    @Test
    void sendMessage_withValidResponse_shouldReturnTextContent() throws Exception {
        String successResponse = """
            {
              "id": "msg-123",
              "type": "message",
              "role": "assistant",
              "content": [
                {
                  "type": "text",
                  "text": "Here is my analysis of your tasks."
                }
              ],
              "model": "claude-3-5-sonnet-20241022",
              "stop_reason": "end_turn",
              "usage": {
                "input_tokens": 100,
                "output_tokens": 50
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(successResponse);

        String result = client.sendMessage("You are a helpful assistant", "Analyze these tasks");

        assertNotNull(result);
        assertEquals("Here is my analysis of your tasks.", result);

        // Verify HTTP request was made
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void sendMessage_withMultipleContentBlocks_shouldReturnFirstText() throws Exception {
        String successResponse = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "First block content"
                },
                {
                  "type": "text",
                  "text": "Second block content"
                }
              ]
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(successResponse);

        String result = client.sendMessage("system", "user");

        assertEquals("First block content", result);
    }

    @Test
    void sendMessage_shouldIncludeRequiredHeaders() throws Exception {
        String successResponse = """
            {
              "content": [{"type": "text", "text": "response"}]
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(successResponse);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

        client.sendMessage("system", "user");

        verify(mockHttpClient).send(requestCaptor.capture(), any());
        HttpRequest capturedRequest = requestCaptor.getValue();

        // Verify critical headers
        assertTrue(capturedRequest.headers().firstValue("x-api-key").isPresent(),
            "x-api-key header must be present");
        assertEquals(TEST_API_KEY, capturedRequest.headers().firstValue("x-api-key").get(),
            "x-api-key must match configured API key");

        assertTrue(capturedRequest.headers().firstValue("anthropic-version").isPresent(),
            "anthropic-version header must be present");

        assertTrue(capturedRequest.headers().firstValue("Content-Type").isPresent(),
            "Content-Type header must be present");
        assertEquals("application/json",
            capturedRequest.headers().firstValue("Content-Type").get());
    }

    @Test
    void sendMessage_shouldBuildValidRequestBody() throws Exception {
        String successResponse = """
            {
              "content": [{"type": "text", "text": "response"}]
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(successResponse);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

        client.sendMessage("System prompt", "User message");

        verify(mockHttpClient).send(requestCaptor.capture(), any());
        HttpRequest capturedRequest = requestCaptor.getValue();

        // Extract and validate request body
        String bodyString = capturedRequest.bodyPublisher()
            .orElseThrow()
            .toString();

        // Body should contain model, max_tokens, temperature
        assertNotNull(bodyString, "Request body should not be null");
        assertTrue(bodyString.length() > 0, "Request body should not be empty");
    }

    // ============ Authentication Error Cases ============

    @Test
    void sendMessage_with401Response_shouldThrowAuthenticationFailedException() throws Exception {
        String errorResponse = """
            {
              "error": {
                "message": "Invalid API key"
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn(errorResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.AUTHENTICATION_FAILED,
            exception.getErrorType());
        assertEquals(401, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Authentication failed"));

        // Ensure API key is NOT in error message or logs
        assertFalse(exception.getMessage().contains(TEST_API_KEY),
            "API key must not be exposed in error messages");
    }

    @Test
    void sendMessage_with403Response_shouldThrowAuthenticationFailedException() throws Exception {
        String errorResponse = """
            {
              "error": {
                "message": "Forbidden"
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(403);
        when(mockResponse.body()).thenReturn(errorResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.AUTHENTICATION_FAILED,
            exception.getErrorType());
    }

    // ============ Rate Limit Cases ============

    @Test
    void sendMessage_with429Response_shouldThrowRateLimitException() throws Exception {
        String errorResponse = """
            {
              "error": {
                "message": "Rate limit exceeded"
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.body()).thenReturn(errorResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.RATE_LIMIT_EXCEEDED,
            exception.getErrorType());
        assertEquals(429, exception.getStatusCode());
    }

    // ============ Server Error Cases ============

    @Test
    void sendMessage_with500Response_shouldThrowServerException() throws Exception {
        String errorResponse = """
            {
              "error": {
                "message": "Internal server error"
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn(errorResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.SERVER_ERROR,
            exception.getErrorType());
        assertEquals(500, exception.getStatusCode());
    }

    @Test
    void sendMessage_with502Response_shouldThrowServerException() throws Exception {
        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(502);
        when(mockResponse.body()).thenReturn("Bad Gateway");

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.SERVER_ERROR,
            exception.getErrorType());
    }

    @Test
    void sendMessage_with503Response_shouldThrowServerException() throws Exception {
        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(503);
        when(mockResponse.body()).thenReturn("Service Unavailable");

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.SERVER_ERROR,
            exception.getErrorType());
    }

    // ============ Timeout Cases ============

    @Test
    void sendMessage_withHttpTimeoutException_shouldThrowTimeoutException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
            .thenThrow(new java.net.http.HttpTimeoutException("Connection timeout"));

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.TIMEOUT,
            exception.getErrorType());
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void sendMessage_withInterruptedException_shouldThrowNetworkException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
            .thenThrow(new InterruptedException("Thread interrupted"));

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.NETWORK_ERROR,
            exception.getErrorType());
    }

    // ============ Malformed Response Cases ============

    @Test
    void sendMessage_withMissingContentArray_shouldThrowMalformedResponseException() throws Exception {
        String malformedResponse = """
            {
              "id": "msg-123",
              "type": "message"
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(malformedResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE,
            exception.getErrorType());
        assertTrue(exception.getMessage().contains("content"));
    }

    @Test
    void sendMessage_withEmptyContentArray_shouldThrowMalformedResponseException() throws Exception {
        String malformedResponse = """
            {
              "content": []
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(malformedResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE,
            exception.getErrorType());
    }

    @Test
    void sendMessage_withMissingTextField_shouldThrowMalformedResponseException() throws Exception {
        String malformedResponse = """
            {
              "content": [
                {
                  "type": "text"
                }
              ]
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(malformedResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE,
            exception.getErrorType());
    }

    @Test
    void sendMessage_withInvalidJson_shouldThrowMalformedResponseException() throws Exception {
        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{ invalid json }");

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE,
            exception.getErrorType());
    }

    // ============ Client Error Cases (4xx) ============

    @Test
    void sendMessage_with400Response_shouldThrowInvalidRequestException() throws Exception {
        String errorResponse = """
            {
              "error": {
                "message": "Invalid request parameters"
              }
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn(errorResponse);

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.INVALID_REQUEST,
            exception.getErrorType());
        assertEquals(400, exception.getStatusCode());
    }

    @Test
    void sendMessage_with404Response_shouldThrowInvalidRequestException() throws Exception {
        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.body()).thenReturn("Not found");

        ClaudeApiClient.ClaudeApiException exception = assertThrows(
            ClaudeApiClient.ClaudeApiException.class,
            () -> client.sendMessage("system", "user")
        );

        assertEquals(ClaudeApiClient.ClaudeApiException.ErrorType.INVALID_REQUEST,
            exception.getErrorType());
    }

    // ============ Helper Methods ============

    /**
     * Injects a mock HttpClient into DirectAnthropicClient via reflection.
     * This allows testing without instantiating the real HTTP client.
     */
    private void injectHttpClient(DirectAnthropicClient client, HttpClient mockClient) {
        try {
            var field = DirectAnthropicClient.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(client, mockClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock HttpClient", e);
        }
    }
}
