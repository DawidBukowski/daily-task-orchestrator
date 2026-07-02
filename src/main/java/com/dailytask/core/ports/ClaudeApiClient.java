package com.dailytask.core.ports;

/**
 * Port interface for communicating with Claude AI API.
 *
 * <p>This interface abstracts the Claude API communication, allowing for:
 * <ul>
 *   <li>Multiple implementation strategies (HTTP, mock, etc.)</li>
 *   <li>Testing with stub implementations</li>
 *   <li>Flexible API provider switching</li>
 * </ul>
 *
 * <p>Implementations should handle:
 * <ul>
 *   <li>Authentication and API key management</li>
 *   <li>HTTP client configuration and retries</li>
 *   <li>Rate limiting and backoff</li>
 *   <li>Error handling and timeout management</li>
 * </ul>
 */
public interface ClaudeApiClient {

    /**
     * Sends a message to Claude AI with system and user prompts.
     *
     * <p>The system prompt defines Claude's behavior and constraints,
     * while the user prompt contains the actual data and request.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Return the raw text response from Claude</li>
     *   <li>Throw exceptions for unrecoverable errors (network, auth)</li>
     *   <li>Handle retries and backoff internally</li>
     *   <li>Log requests/responses appropriately (without exposing secrets)</li>
     * </ul>
     *
     * @param systemPrompt the system-level instructions for Claude (must not be null)
     * @param userPrompt the user message with data to analyze (must not be null)
     * @return the raw text response from Claude API
     * @throws IllegalArgumentException if either prompt is null
     * @throws ClaudeApiException if the API request fails
     */
    String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiException;

    /**
     * Exception thrown when Claude API communication fails.
     *
     * <p>This exception wraps various failure modes:
     * <ul>
     *   <li>Network connectivity issues</li>
     *   <li>Authentication failures</li>
     *   <li>Rate limiting errors</li>
     *   <li>API errors or timeouts</li>
     * </ul>
     */
    class ClaudeApiException extends Exception {

        public enum ErrorType {
            AUTHENTICATION_FAILED,
            RATE_LIMIT_EXCEEDED,
            SERVER_ERROR,
            TIMEOUT,
            MALFORMED_RESPONSE,
            NETWORK_ERROR,
            INVALID_REQUEST
        }

        private final ErrorType errorType;
        private final int statusCode;

        public ClaudeApiException(String message) {
            super(message);
            this.errorType = ErrorType.NETWORK_ERROR;
            this.statusCode = -1;
        }

        public ClaudeApiException(String message, Throwable cause) {
            super(message, cause);
            this.errorType = ErrorType.NETWORK_ERROR;
            this.statusCode = -1;
        }

        public ClaudeApiException(String message, ErrorType errorType) {
            super(message);
            this.errorType = errorType;
            this.statusCode = -1;
        }

        public ClaudeApiException(String message, ErrorType errorType, int statusCode) {
            super(message);
            this.errorType = errorType;
            this.statusCode = statusCode;
        }

        public ClaudeApiException(String message, ErrorType errorType, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
            this.statusCode = -1;
        }

        public ClaudeApiException(String message, ErrorType errorType, int statusCode, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
            this.statusCode = statusCode;
        }

        public ErrorType getErrorType() {
            return errorType;
        }

        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ClaudeApiException{");
            sb.append("errorType=").append(errorType);
            if (statusCode != -1) {
                sb.append(", statusCode=").append(statusCode);
            }
            sb.append(", message='").append(getMessage()).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
