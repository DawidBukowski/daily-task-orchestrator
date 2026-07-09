package com.dailytask.adapters.datasources.gmail;

import com.google.api.client.auth.oauth2.StoredCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretsManagerDataStoreTest {

    @Mock
    private SecretsManagerClient mockSecretsClient;

    @Mock
    private SecretsManagerDataStoreFactory mockFactory;

    private SecretsManagerDataStore<StoredCredential> dataStore;

    @BeforeEach
    void setUp() {
        // Mock GetSecretValue to return empty secret initially (no tokens exist yet)
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenThrow(ResourceNotFoundException.class);

        dataStore = new SecretsManagerDataStore<>(
            mockFactory,
            "StoredCredential",
            mockSecretsClient,
            "daily-task-orchestrator"
        );
    }

    @Test
    void constructor_InitializesWithCorrectSecretName() {
        assertEquals("daily-task-orchestrator/gmail-tokens/StoredCredential",
                    dataStore.getSecretName());
    }

    @Test
    void get_NullKey_ReturnsNull() throws IOException {
        StoredCredential result = dataStore.get(null);
        assertNull(result, "Should return null for null key");
    }

    @Test
    void get_BlankKey_ReturnsNull() throws IOException {
        StoredCredential result = dataStore.get("   ");
        assertNull(result, "Should return null for blank key");
    }

    @Test
    void get_NonexistentKey_ReturnsNull() throws IOException {
        StoredCredential result = dataStore.get("nonexistent-key");
        assertNull(result, "Should return null for nonexistent key");
    }

    @Test
    void set_NewCredential_CreatesSecret() throws IOException {
        // Arrange
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("test-access-token");
        credential.setRefreshToken("test-refresh-token");
        credential.setExpirationTimeMilliseconds(1234567890123L);

        // Mock CreateSecret success (secret doesn't exist yet)
        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenThrow(ResourceNotFoundException.class);
        when(mockSecretsClient.createSecret(any(CreateSecretRequest.class)))
            .thenReturn(CreateSecretResponse.builder().build());

        // Act
        dataStore.set("user", credential);

        // Assert
        ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
        verify(mockSecretsClient).createSecret(captor.capture());

        CreateSecretRequest request = captor.getValue();
        assertEquals("daily-task-orchestrator/gmail-tokens/StoredCredential", request.name());
        assertTrue(request.secretString().contains("test-access-token"));
        assertTrue(request.secretString().contains("test-refresh-token"));
    }

    @Test
    void set_ExistingCredential_UpdatesSecret() throws IOException {
        // Arrange
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("updated-access-token");
        credential.setRefreshToken("updated-refresh-token");
        credential.setExpirationTimeMilliseconds(9876543210123L);

        // Mock UpdateSecret success (secret already exists)
        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        // Act
        dataStore.set("user", credential);

        // Assert
        ArgumentCaptor<UpdateSecretRequest> captor = ArgumentCaptor.forClass(UpdateSecretRequest.class);
        verify(mockSecretsClient).updateSecret(captor.capture());

        UpdateSecretRequest request = captor.getValue();
        assertEquals("daily-task-orchestrator/gmail-tokens/StoredCredential", request.secretId());
        assertTrue(request.secretString().contains("updated-access-token"));
        assertTrue(request.secretString().contains("updated-refresh-token"));
    }

    @Test
    void set_NullKey_ThrowsException() {
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("test-token");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dataStore.set(null, credential)
        );

        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void set_NullValue_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dataStore.set("user", null)
        );

        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void get_AfterSet_ReturnsStoredCredential() throws IOException {
        // Arrange
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("test-access-token");
        credential.setRefreshToken("test-refresh-token");
        credential.setExpirationTimeMilliseconds(1234567890123L);

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        // Act
        dataStore.set("user", credential);
        StoredCredential retrieved = dataStore.get("user");

        // Assert
        assertNotNull(retrieved);
        assertEquals("test-access-token", retrieved.getAccessToken());
        assertEquals("test-refresh-token", retrieved.getRefreshToken());
        assertEquals(1234567890123L, retrieved.getExpirationTimeMilliseconds());
    }

    @Test
    void delete_ExistingKey_RemovesFromCache() throws IOException {
        // Arrange
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("test-token");

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        dataStore.set("user", credential);

        // Act
        dataStore.delete("user");

        // Assert
        assertNull(dataStore.get("user"), "Deleted key should return null");
        verify(mockSecretsClient, times(1)).deleteSecret(any(DeleteSecretRequest.class));
    }

    @Test
    void delete_NullKey_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dataStore.delete(null)
        );

        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void clear_RemovesAllCredentials() throws IOException {
        // Arrange
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");

        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        when(mockSecretsClient.deleteSecret(any(DeleteSecretRequest.class)))
            .thenReturn(DeleteSecretResponse.builder().build());

        // Act
        dataStore.clear();

        // Assert
        assertEquals(0, dataStore.getCacheSize(), "Cache should be empty after clear");
        verify(mockSecretsClient, times(1)).deleteSecret(any(DeleteSecretRequest.class));
    }

    @Test
    void keySet_EmptyStore_ReturnsEmptySet() throws IOException {
        Set<String> keys = dataStore.keySet();

        assertNotNull(keys);
        assertTrue(keys.isEmpty(), "Should return empty set for empty store");
    }

    @Test
    void keySet_AfterSet_ReturnsAllKeys() throws IOException {
        // Arrange
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");

        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // Act
        Set<String> keys = dataStore.keySet();

        // Assert
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("user1"));
        assertTrue(keys.contains("user2"));
    }

    @Test
    void values_EmptyStore_ReturnsEmptyCollection() throws IOException {
        Collection<StoredCredential> values = dataStore.values();

        assertNotNull(values);
        assertTrue(values.isEmpty(), "Should return empty collection for empty store");
    }

    @Test
    void values_AfterSet_ReturnsAllValues() throws IOException {
        // Arrange
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");

        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // Act
        Collection<StoredCredential> values = dataStore.values();

        // Assert
        assertNotNull(values);
        assertEquals(2, values.size());
    }

    @Test
    void getCacheSize_InitiallyZero() {
        assertEquals(0, dataStore.getCacheSize(), "Cache should be empty initially");
    }

    @Test
    void getCacheSize_AfterSet_ReturnsCorrectSize() throws IOException {
        // Arrange
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("token");

        when(mockSecretsClient.updateSecret(any(UpdateSecretRequest.class)))
            .thenReturn(UpdateSecretResponse.builder().build());

        // Act
        dataStore.set("user", credential);

        // Assert
        assertEquals(1, dataStore.getCacheSize(), "Cache should contain 1 credential");
    }

    @Test
    void loadAllFromSecretsManager_ValidJson_PopulatesCache() throws IOException {
        // Arrange
        String validJson = "{\"user\":{\"accessToken\":\"test-token\",\"refreshToken\":\"refresh-token\",\"expirationTimeMillis\":1234567890123}}";

        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString(validJson)
            .build();

        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Create a new dataStore to trigger loading
        SecretsManagerDataStore<StoredCredential> newDataStore = new SecretsManagerDataStore<>(
            mockFactory,
            "StoredCredential",
            mockSecretsClient,
            "daily-task-orchestrator"
        );

        // Act
        StoredCredential credential = newDataStore.get("user");

        // Assert
        assertNotNull(credential);
        assertEquals("test-token", credential.getAccessToken());
        assertEquals("refresh-token", credential.getRefreshToken());
        assertEquals(1234567890123L, credential.getExpirationTimeMilliseconds());
    }

    @Test
    void loadAllFromSecretsManager_EmptyJson_DoesNotThrow() throws IOException {
        // Arrange
        GetSecretValueResponse response = GetSecretValueResponse.builder()
            .secretString("{}")
            .build();

        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(response);

        // Act & Assert - should not throw
        SecretsManagerDataStore<StoredCredential> newDataStore = new SecretsManagerDataStore<>(
            mockFactory,
            "StoredCredential",
            mockSecretsClient,
            "daily-task-orchestrator"
        );

        assertEquals(0, newDataStore.getCacheSize());
    }
}
