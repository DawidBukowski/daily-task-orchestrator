package com.dailytask.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaudeConfiguration.
 * Tests environment variable parsing, validation, and builder pattern.
 */
class ClaudeConfigurationTest {

    @AfterEach
    void tearDown() {
        // Clean up any modified environment variables
        clearEnvironmentVariable("CLAUDE_PROVIDER");
        clearEnvironmentVariable("CLAUDE_MODEL_ID");
        clearEnvironmentVariable("ANTHROPIC_API_KEY");
        clearEnvironmentVariable("AWS_REGION");
        clearEnvironmentVariable("CLAUDE_MAX_TOKENS");
        clearEnvironmentVariable("CLAUDE_TEMPERATURE");
        clearEnvironmentVariable("CLAUDE_TIMEOUT_SECONDS");
        clearEnvironmentVariable("ANTHROPIC_API_URL");
    }

    @Test
    void builder_shouldCreateValidAnthropicConfiguration() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(60)
            .anthropicApiKey("test-key")
            .anthropicApiUrl("https://api.anthropic.com/v1/messages")
            .build();

        assertEquals(ClaudeConfiguration.Provider.ANTHROPIC, config.getProvider());
        assertEquals("claude-3-5-sonnet-20241022", config.getModelId());
        assertEquals(2000, config.getMaxTokens());
        assertEquals(0.7, config.getTemperature());
        assertEquals(60, config.getTimeoutSeconds());
        assertEquals("test-key", config.getAnthropicApiKey());
        assertEquals("https://api.anthropic.com/v1/messages", config.getAnthropicApiUrl());
    }

    @Test
    void builder_shouldCreateValidAwsBedrockConfiguration() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.AWS_BEDROCK)
            .modelId("anthropic.claude-3-5-sonnet-20241022-v2:0")
            .maxTokens(1500)
            .temperature(0.5)
            .timeoutSeconds(45)
            .awsRegion("us-east-1")
            .build();

        assertEquals(ClaudeConfiguration.Provider.AWS_BEDROCK, config.getProvider());
        assertEquals("anthropic.claude-3-5-sonnet-20241022-v2:0", config.getModelId());
        assertEquals(1500, config.getMaxTokens());
        assertEquals(0.5, config.getTemperature());
        assertEquals(45, config.getTimeoutSeconds());
        assertEquals("us-east-1", config.getAwsRegion());
    }

    @Test
    void builder_shouldUseDefaultValues() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .anthropicApiKey("test-key")
            .build();

        assertEquals(1000, config.getMaxTokens());
        assertEquals(0.3, config.getTemperature());
        assertEquals(30, config.getTimeoutSeconds());
        assertEquals("https://api.anthropic.com/v1/messages", config.getAnthropicApiUrl());
    }

    @Test
    void builder_shouldThrowExceptionForNullProvider() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .modelId("claude-3-5-sonnet-20241022")
            .anthropicApiKey("test-key");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForNullModelId() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .anthropicApiKey("test-key");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForBlankModelId() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("")
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForMissingAnthropicApiKey() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForMissingAwsRegion() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.AWS_BEDROCK)
            .modelId("anthropic.claude-3-5-sonnet-20241022-v2:0");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForNegativeMaxTokens() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .maxTokens(-1)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForInvalidTemperature() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .temperature(1.5)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_shouldThrowExceptionForNegativeTimeout() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .timeoutSeconds(-1)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionWhenProviderMissing() {
        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionWhenModelIdMissing() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionForInvalidProvider() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "INVALID");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    // ============ Environment Variable Tests - ANTHROPIC Provider ============

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldThrowWhenApiKeyMissing() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldThrowWhenApiKeyBlank() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldLoadApiKey() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key-sk-12345");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals("test-key-sk-12345", config.getAnthropicApiKey());
        assertEquals(ClaudeConfiguration.Provider.ANTHROPIC, config.getProvider());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldUseCustomApiUrl() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("ANTHROPIC_API_URL", "https://custom.anthropic.com/messages");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals("https://custom.anthropic.com/messages", config.getAnthropicApiUrl());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldUseDefaultApiUrlWhenNotProvided() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        clearEnvironmentVariable("ANTHROPIC_API_URL");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals("https://api.anthropic.com/v1/messages", config.getAnthropicApiUrl());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAnthropicProvider_shouldUseDefaultApiUrlWhenBlank() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("ANTHROPIC_API_URL", "   ");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals("https://api.anthropic.com/v1/messages", config.getAnthropicApiUrl());
    }

    // ============ Environment Variable Tests - AWS_BEDROCK Provider ============

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAwsBedrockProvider_shouldThrowWhenRegionMissing() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "AWS_BEDROCK");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "anthropic.claude-3-5-sonnet-20241022-v2:0");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAwsBedrockProvider_shouldThrowWhenRegionBlank() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "AWS_BEDROCK");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "anthropic.claude-3-5-sonnet-20241022-v2:0");
        setEnvironmentVariable("AWS_REGION", "");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_withAwsBedrockProvider_shouldLoadRegion() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "AWS_BEDROCK");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "anthropic.claude-3-5-sonnet-20241022-v2:0");
        setEnvironmentVariable("AWS_REGION", "us-west-2");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals("us-west-2", config.getAwsRegion());
        assertEquals(ClaudeConfiguration.Provider.AWS_BEDROCK, config.getProvider());
    }

    // ============ Environment Variable Tests - Optional Parameters ============

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldParseMaxTokensFromEnvironment() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_MAX_TOKENS", "5000");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(5000, config.getMaxTokens());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldUseDefaultMaxTokensWhenNotProvided() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        clearEnvironmentVariable("CLAUDE_MAX_TOKENS");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(1000, config.getMaxTokens());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionForInvalidMaxTokens() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_MAX_TOKENS", "not-a-number");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldParseTemperatureFromEnvironment() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_TEMPERATURE", "0.5");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(0.5, config.getTemperature());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldUseDefaultTemperatureWhenNotProvided() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        clearEnvironmentVariable("CLAUDE_TEMPERATURE");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(0.3, config.getTemperature());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionForInvalidTemperature() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_TEMPERATURE", "not-a-number");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldParseTimeoutSecondsFromEnvironment() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_TIMEOUT_SECONDS", "60");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldUseDefaultTimeoutWhenNotProvided() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        clearEnvironmentVariable("CLAUDE_TIMEOUT_SECONDS");

        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

        assertEquals(30, config.getTimeoutSeconds());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires OS-level environment variable manipulation")
    void fromEnv_shouldThrowExceptionForInvalidTimeout() {
        setEnvironmentVariable("CLAUDE_PROVIDER", "ANTHROPIC");
        setEnvironmentVariable("CLAUDE_MODEL_ID", "claude-3-5-sonnet-20241022");
        setEnvironmentVariable("ANTHROPIC_API_KEY", "test-key");
        setEnvironmentVariable("CLAUDE_TIMEOUT_SECONDS", "invalid");

        assertThrows(IllegalStateException.class, ClaudeConfiguration::fromEnv);
    }

    // ============ Validation Tests ============

    @Test
    void validation_shouldRejectZeroMaxTokens() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .maxTokens(0)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void validation_shouldRejectNegativeTemperature() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .temperature(-0.1)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void validation_shouldRejectTemperatureGreaterThanOne() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .temperature(1.1)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void validation_shouldAcceptTemperatureOfZero() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .temperature(0.0)
            .anthropicApiKey("test-key")
            .build();

        assertEquals(0.0, config.getTemperature());
    }

    @Test
    void validation_shouldAcceptTemperatureOfOne() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .temperature(1.0)
            .anthropicApiKey("test-key")
            .build();

        assertEquals(1.0, config.getTemperature());
    }

    @Test
    void validation_shouldRejectZeroTimeout() {
        ClaudeConfiguration.Builder builder = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .timeoutSeconds(0)
            .anthropicApiKey("test-key");

        assertThrows(IllegalStateException.class, builder::build);
    }

    /**
     * Helper method to set environment variables for testing.
     * Uses reflection to modify the environment map.
     */
    @SuppressWarnings("unchecked")
    private void setEnvironmentVariable(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set environment variable", e);
        }
    }

    /**
     * Helper method to clear environment variables after testing.
     */
    @SuppressWarnings("unchecked")
    private void clearEnvironmentVariable(String key) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).remove(key);
        } catch (Exception e) {
            // Ignore - variable might not exist
        }
    }
}
