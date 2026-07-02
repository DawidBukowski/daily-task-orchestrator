package com.dailytask.core.config;

import java.util.Objects;

/**
 * Configuration for Claude AI API integration supporting multiple providers.
 * Supports both direct Anthropic API and AWS Bedrock runtime.
 */
public class ClaudeConfiguration {

    public enum Provider {
        ANTHROPIC,
        AWS_BEDROCK
    }

    private final Provider provider;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    private final int timeoutSeconds;

    // Anthropic-specific configuration
    private final String anthropicApiKey;
    private final String anthropicApiUrl;

    // AWS-specific configuration
    private final String awsRegion;

    private ClaudeConfiguration(Builder builder) {
        this.provider = Objects.requireNonNull(builder.provider, "Provider cannot be null");
        this.modelId = Objects.requireNonNull(builder.modelId, "Model ID cannot be null");
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.anthropicApiKey = builder.anthropicApiKey;
        this.anthropicApiUrl = builder.anthropicApiUrl;
        this.awsRegion = builder.awsRegion;

        validate();
    }

    private void validate() {
        if (modelId.isBlank()) {
            throw new IllegalStateException("Model ID cannot be blank");
        }

        if (provider == Provider.ANTHROPIC) {
            if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
                throw new IllegalStateException(
                    "ANTHROPIC_API_KEY environment variable is required when using ANTHROPIC provider"
                );
            }
        }

        if (provider == Provider.AWS_BEDROCK) {
            if (awsRegion == null || awsRegion.isBlank()) {
                throw new IllegalStateException(
                    "AWS_REGION environment variable is required when using AWS_BEDROCK provider"
                );
            }
        }

        if (maxTokens <= 0) {
            throw new IllegalStateException("Max tokens must be positive");
        }

        if (temperature < 0.0 || temperature > 1.0) {
            throw new IllegalStateException("Temperature must be between 0.0 and 1.0");
        }

        if (timeoutSeconds <= 0) {
            throw new IllegalStateException("Timeout must be positive");
        }
    }

    /**
     * Creates a ClaudeConfiguration from environment variables.
     *
     * Required environment variables:
     * - CLAUDE_PROVIDER: either "ANTHROPIC" or "AWS_BEDROCK"
     * - CLAUDE_MODEL_ID: the Claude model identifier
     *
     * Provider-specific required variables:
     * - For ANTHROPIC: ANTHROPIC_API_KEY
     * - For AWS_BEDROCK: AWS_REGION
     *
     * Optional environment variables:
     * - CLAUDE_MAX_TOKENS: defaults to 1000
     * - CLAUDE_TEMPERATURE: defaults to 0.3
     * - CLAUDE_TIMEOUT_SECONDS: defaults to 30
     * - ANTHROPIC_API_URL: defaults to https://api.anthropic.com/v1/messages
     */
    public static ClaudeConfiguration fromEnv() {
        String providerStr = System.getenv("CLAUDE_PROVIDER");
        if (providerStr == null || providerStr.isBlank()) {
            throw new IllegalStateException(
                "CLAUDE_PROVIDER environment variable is required. Valid values: ANTHROPIC, AWS_BEDROCK"
            );
        }

        Provider provider;
        try {
            provider = Provider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Invalid CLAUDE_PROVIDER value: " + providerStr + ". Valid values: ANTHROPIC, AWS_BEDROCK"
            );
        }

        String modelId = System.getenv("CLAUDE_MODEL_ID");
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalStateException("CLAUDE_MODEL_ID environment variable is required");
        }

        Builder builder = new Builder()
            .provider(provider)
            .modelId(modelId)
            .maxTokens(getEnvInt("CLAUDE_MAX_TOKENS", 1000))
            .temperature(getEnvDouble("CLAUDE_TEMPERATURE", 0.3))
            .timeoutSeconds(getEnvInt("CLAUDE_TIMEOUT_SECONDS", 30));

        if (provider == Provider.ANTHROPIC) {
            builder.anthropicApiKey(System.getenv("ANTHROPIC_API_KEY"))
                   .anthropicApiUrl(getEnvOrDefault(
                       "ANTHROPIC_API_URL",
                       "https://api.anthropic.com/v1/messages"
                   ));
        }

        if (provider == Provider.AWS_BEDROCK) {
            builder.awsRegion(System.getenv("AWS_REGION"));
        }

        return builder.build();
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "Invalid integer value for " + key + ": " + value
            );
        }
    }

    private static double getEnvDouble(String key, double defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "Invalid double value for " + key + ": " + value
            );
        }
    }

    // Getters
    public Provider getProvider() { return provider; }
    public String getModelId() { return modelId; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public String getAnthropicApiKey() { return anthropicApiKey; }
    public String getAnthropicApiUrl() { return anthropicApiUrl; }
    public String getAwsRegion() { return awsRegion; }

    public static class Builder {
        private Provider provider;
        private String modelId;
        private int maxTokens = 1000;
        private double temperature = 0.3;
        private int timeoutSeconds = 30;
        private String anthropicApiKey;
        private String anthropicApiUrl = "https://api.anthropic.com/v1/messages";
        private String awsRegion;

        public Builder provider(Provider provider) {
            this.provider = provider;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder anthropicApiKey(String anthropicApiKey) {
            this.anthropicApiKey = anthropicApiKey;
            return this;
        }

        public Builder anthropicApiUrl(String anthropicApiUrl) {
            this.anthropicApiUrl = anthropicApiUrl;
            return this;
        }

        public Builder awsRegion(String awsRegion) {
            this.awsRegion = awsRegion;
            return this;
        }

        public ClaudeConfiguration build() {
            return new ClaudeConfiguration(this);
        }
    }
}
