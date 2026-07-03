package com.dailytask.adapters.secrets;

import com.dailytask.core.ports.SecretsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsSecretsManagerProviderTest {

    @Mock
    private SecretsManagerClient mockSecretsClient;

    private AwsSecretsManagerProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwsSecretsManagerProvider(mockSecretsClient, "us-east-1");
    }

    @Test
    void constructor_NullRegion_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                    () -> new AwsSecretsManagerProvider(null),
                    "Should throw exception for null region");
    }

    @Test
    void constructor_BlankRegion_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                    () -> new AwsSecretsManagerProvider("   "),
                    "Should throw exception for blank region");
    }

    @Test
    void constructor_NullClient_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                    () -> new AwsSecretsManagerProvider(null, "us-east-1"),
                    "Should throw exception for null client");
    }

    @Test
    void getSecret_Success_ReturnsValue() {
        // Arrange
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString("my-secret-value")
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act
        Optional<String> result = provider.getSecret("test-key");

        // Assert
        assertTrue(result.isPresent(), "Should return secret value");
        assertEquals("my-secret-value", result.get());
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getSecret_NotFound_ReturnsEmpty() {
        // Arrange
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenThrow(ResourceNotFoundException.class);

        // Act
        Optional<String> result = provider.getSecret("nonexistent-key");

        // Assert
        assertFalse(result.isPresent(), "Should return empty for nonexistent secret");
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getSecret_NullKey_ReturnsEmpty() {
        // Act
        Optional<String> result = provider.getSecret(null);

        // Assert
        assertFalse(result.isPresent(), "Should return empty for null key");
        verify(mockSecretsClient, never()).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getSecret_BlankKey_ReturnsEmpty() {
        // Act
        Optional<String> result = provider.getSecret("   ");

        // Assert
        assertFalse(result.isPresent(), "Should return empty for blank key");
        verify(mockSecretsClient, never()).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getSecret_Caching_ReducesApiCalls() {
        // Arrange
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString("cached-value")
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act - call twice with same key
        Optional<String> first = provider.getSecret("test-key");
        Optional<String> second = provider.getSecret("test-key");

        // Assert
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get(), second.get(), "Should return same value");
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getStructuredSecret_Success_ReturnsMap() throws Exception {
        // Arrange
        String secretJson = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString(secretJson)
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act
        Map<String, String> result = provider.getStructuredSecret("my-secret");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
    }

    @Test
    void getStructuredSecret_NotFound_ThrowsException() {
        // Arrange
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenThrow(ResourceNotFoundException.class);

        // Act & Assert
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret("nonexistent-secret")
        );
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void getStructuredSecret_NullName_ThrowsException() {
        // Act & Assert
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret(null)
        );
        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void getStructuredSecret_BlankName_ThrowsException() {
        // Act & Assert
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret("   ")
        );
        assertTrue(exception.getMessage().contains("must not be null or blank"));
    }

    @Test
    void getStructuredSecret_EmptyValue_ThrowsException() {
        // Arrange
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString("")
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act & Assert
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret("empty-secret")
        );
        assertTrue(exception.getMessage().contains("empty value"));
    }

    @Test
    void getStructuredSecret_InvalidJson_ThrowsException() {
        // Arrange
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString("not valid json {{{")
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act & Assert
        SecretsProvider.SecretsException exception = assertThrows(
            SecretsProvider.SecretsException.class,
            () -> provider.getStructuredSecret("invalid-json-secret")
        );
        assertTrue(exception.getMessage().contains("Failed to retrieve or parse"));
    }

    @Test
    void getStructuredSecret_CachesIndividualKeys() throws Exception {
        // Arrange
        String secretJson = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString(secretJson)
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act
        provider.getStructuredSecret("my-secret");
        Optional<String> cachedValue = provider.getSecret("key1");

        // Assert
        assertTrue(cachedValue.isPresent(), "Individual keys should be cached");
        assertEquals("value1", cachedValue.get());
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void getAllSecrets_ReturnsCache() throws Exception {
        // Arrange
        String secretJson = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString(secretJson)
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act
        provider.getStructuredSecret("my-secret");
        Map<String, String> allSecrets = provider.getAllSecrets();

        // Assert
        assertNotNull(allSecrets);
        assertEquals(2, allSecrets.size());
        assertTrue(allSecrets.containsKey("key1"));
        assertTrue(allSecrets.containsKey("key2"));
    }

    @Test
    void getRegion_ReturnsConfiguredRegion() {
        assertEquals("us-east-1", provider.getRegion());
    }

    @Test
    void getCacheSize_ReturnsCorrectSize() throws Exception {
        // Arrange
        String secretJson = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString(secretJson)
            .build();
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act
        assertEquals(0, provider.getCacheSize(), "Cache should be empty initially");
        provider.getStructuredSecret("my-secret");

        // Assert
        assertEquals(3, provider.getCacheSize(), "Cache should contain 3 keys");
    }

    @Test
    void close_ClosesClient() {
        // Act
        provider.close();

        // Assert
        verify(mockSecretsClient, times(1)).close();
    }
}
