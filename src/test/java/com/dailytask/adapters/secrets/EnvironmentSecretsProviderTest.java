package com.dailytask.adapters.secrets;

import com.dailytask.core.ports.SecretsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSecretsProviderTest {

    private EnvironmentSecretsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new EnvironmentSecretsProvider();
    }

    @Test
    void getSecret_ExistingEnvironmentVariable_ReturnsValue() {
        // Use a known environment variable that should exist on most systems
        String key = System.getenv().keySet().iterator().next();

        Optional<String> result = provider.getSecret(key);

        assertTrue(result.isPresent(), "Should find existing environment variable");
        assertNotNull(result.get());
        assertFalse(result.get().isBlank());
    }

    @Test
    void getSecret_NonexistentVariable_ReturnsEmpty() {
        String nonexistentKey = "NONEXISTENT_SECRET_KEY_12345";

        Optional<String> result = provider.getSecret(nonexistentKey);

        assertFalse(result.isPresent(), "Should return empty for nonexistent variable");
    }

    @Test
    void getSecret_NullKey_ReturnsEmpty() {
        Optional<String> result = provider.getSecret(null);

        assertFalse(result.isPresent(), "Should return empty for null key");
    }

    @Test
    void getSecret_BlankKey_ReturnsEmpty() {
        Optional<String> result = provider.getSecret("   ");

        assertFalse(result.isPresent(), "Should return empty for blank key");
    }

    @Test
    void getAllSecrets_ReturnsNonEmptyMap() {
        Map<String, String> secrets = provider.getAllSecrets();

        assertNotNull(secrets, "Should never return null");
        assertFalse(secrets.isEmpty(), "Should return environment variables");
    }

    @Test
    void getAllSecrets_ContainsSystemVariables() {
        Map<String, String> secrets = provider.getAllSecrets();

        // PATH should exist on all systems (Windows, Linux, macOS)
        String pathKey = System.getProperty("os.name").toLowerCase().contains("win") ? "PATH" : "PATH";
        assertTrue(secrets.containsKey(pathKey) || secrets.containsKey("Path"),
                   "Should contain PATH environment variable");
    }

    @Test
    void getStructuredSecret_ThrowsException() {
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret("some-secret"),
            "Should throw exception for unsupported operation"
        );

        assertTrue(exception.getMessage().contains("not supported"),
                   "Exception message should explain operation is not supported");
        assertTrue(exception.getMessage().contains("AwsSecretsManagerProvider"),
                   "Exception message should suggest alternative provider");
    }

    @Test
    void getSecret_SameKeyMultipleTimes_ReturnsSameValue() {
        String key = System.getenv().keySet().iterator().next();

        Optional<String> first = provider.getSecret(key);
        Optional<String> second = provider.getSecret(key);

        assertEquals(first, second, "Should return consistent values for same key");
    }
}
