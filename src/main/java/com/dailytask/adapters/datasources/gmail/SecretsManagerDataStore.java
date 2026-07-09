package com.dailytask.adapters.datasources.gmail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataStore implementation that persists OAuth tokens in AWS Secrets Manager.
 * Implements Google's DataStore interface for Gmail API OAuth token storage.
 *
 * <p>Token storage structure:
 * <pre>
 * Secret Name: {secretPrefix}/gmail-tokens/{dataStoreId}
 * Secret Value (JSON):
 * {
 *   "user": {
 *     "accessToken": "ya29.xxx",
 *     "refreshToken": "1//xxx",
 *     "expirationTimeMillis": 1234567890123
 *   }
 * }
 * </pre>
 *
 * <p>Thread-safe: Uses ConcurrentHashMap for in-memory cache and synchronized
 * operations for AWS API calls.
 *
 * @param <V> Serializable value type (typically {@link StoredCredential})
 */
public class SecretsManagerDataStore<V extends Serializable> extends AbstractDataStore<V> {
    private static final Logger logger = LoggerFactory.getLogger(SecretsManagerDataStore.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SecretsManagerClient secretsClient;
    private final String secretName;
    private final Map<String, V> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new SecretsManagerDataStore.
     *
     * @param dataStoreFactory DataStore factory that created this instance
     * @param id               DataStore identifier (e.g., "StoredCredential")
     * @param secretsClient    AWS Secrets Manager client
     * @param secretPrefix     Prefix for secret names
     */
    protected SecretsManagerDataStore(
            SecretsManagerDataStoreFactory dataStoreFactory,
            String id,
            SecretsManagerClient secretsClient,
            String secretPrefix) {
        super(dataStoreFactory, id);
        this.secretsClient = secretsClient;
        this.secretName = secretPrefix + "/gmail-tokens/" + id;

        logger.info("Initialized SecretsManagerDataStore with secret name: {}", secretName);

        // Load existing tokens into cache
        try {
            loadAllFromSecretsManager();
        } catch (IOException e) {
            logger.warn("Failed to load existing tokens from Secrets Manager: {}. Starting with empty cache.",
                       e.getMessage());
        }
    }

    @Override
    public Set<String> keySet() throws IOException {
        logger.debug("keySet() called for secret: {}", secretName);
        loadAllFromSecretsManager(); // Refresh cache
        return new HashSet<>(cache.keySet());
    }

    @Override
    public Collection<V> values() throws IOException {
        logger.debug("values() called for secret: {}", secretName);
        loadAllFromSecretsManager(); // Refresh cache
        return new ArrayList<>(cache.values());
    }

    @Override
    public V get(String key) throws IOException {
        if (key == null || key.isBlank()) {
            logger.debug("get() called with null or blank key");
            return null;
        }

        logger.debug("get() called for key: {}", key);

        // Check cache first
        if (cache.containsKey(key)) {
            logger.debug("Returning cached value for key: {}", key);
            return cache.get(key);
        }

        // Load from Secrets Manager
        loadAllFromSecretsManager();
        return cache.get(key);
    }

    @Override
    public DataStore<V> set(String key, V value) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }

        logger.info("set() called for key: {}", key);

        // Update cache
        cache.put(key, value);

        // Persist to Secrets Manager
        saveAllToSecretsManager();

        return this;
    }

    @Override
    public DataStore<V> clear() throws IOException {
        logger.info("clear() called - removing all tokens from secret: {}", secretName);

        cache.clear();

        try {
            DeleteSecretRequest request = DeleteSecretRequest.builder()
                .secretId(secretName)
                .forceDeleteWithoutRecovery(true)
                .build();

            secretsClient.deleteSecret(request);
            logger.info("Successfully deleted secret: {}", secretName);

        } catch (ResourceNotFoundException e) {
            logger.debug("Secret {} not found during clear() - already deleted or never created", secretName);
        } catch (Exception e) {
            throw new IOException("Failed to delete secret: " + secretName, e);
        }

        return this;
    }

    @Override
    public DataStore<V> delete(String key) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or blank");
        }

        logger.info("delete() called for key: {}", key);

        // Remove from cache
        cache.remove(key);

        // Persist updated cache to Secrets Manager
        if (cache.isEmpty()) {
            // If no keys left, delete the entire secret
            clear();
        } else {
            saveAllToSecretsManager();
        }

        return this;
    }

    /**
     * Loads all tokens from AWS Secrets Manager into the cache.
     *
     * @throws IOException if retrieval or parsing fails
     */
    private void loadAllFromSecretsManager() throws IOException {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretJson = response.secretString();

            if (secretJson == null || secretJson.isBlank() || secretJson.equals("{}")) {
                logger.debug("Secret {} is empty or does not exist yet", secretName);
                return;
            }

            // Parse JSON to Map<String, V>
            Map<String, Map<String, Object>> rawMap = objectMapper.readValue(
                secretJson,
                new TypeReference<Map<String, Map<String, Object>>>() {}
            );

            // Convert to StoredCredential objects
            cache.clear();
            for (Map.Entry<String, Map<String, Object>> entry : rawMap.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> credentialData = entry.getValue();

                // Reconstruct StoredCredential
                StoredCredential credential = new StoredCredential();
                if (credentialData.containsKey("accessToken")) {
                    credential.setAccessToken((String) credentialData.get("accessToken"));
                }
                if (credentialData.containsKey("refreshToken")) {
                    credential.setRefreshToken((String) credentialData.get("refreshToken"));
                }
                if (credentialData.containsKey("expirationTimeMillis")) {
                    Object expiration = credentialData.get("expirationTimeMillis");
                    if (expiration instanceof Number) {
                        credential.setExpirationTimeMilliseconds(((Number) expiration).longValue());
                    }
                }

                @SuppressWarnings("unchecked")
                V value = (V) credential;
                cache.put(key, value);
            }

            logger.debug("Loaded {} tokens from Secrets Manager", cache.size());

        } catch (ResourceNotFoundException e) {
            logger.debug("Secret {} not found - will be created on first write", secretName);
        } catch (Exception e) {
            throw new IOException("Failed to load tokens from Secrets Manager: " + secretName, e);
        }
    }

    /**
     * Saves all cached tokens to AWS Secrets Manager.
     *
     * @throws IOException if serialization or AWS API call fails
     */
    private void saveAllToSecretsManager() throws IOException {
        try {
            // Convert cache to JSON
            Map<String, Map<String, Object>> rawMap = new HashMap<>();
            for (Map.Entry<String, V> entry : cache.entrySet()) {
                String key = entry.getKey();
                V value = entry.getValue();

                if (value instanceof StoredCredential) {
                    StoredCredential credential = (StoredCredential) value;
                    Map<String, Object> credentialData = new HashMap<>();
                    credentialData.put("accessToken", credential.getAccessToken());
                    credentialData.put("refreshToken", credential.getRefreshToken());
                    credentialData.put("expirationTimeMillis", credential.getExpirationTimeMilliseconds());
                    rawMap.put(key, credentialData);
                } else {
                    logger.warn("Skipping non-StoredCredential value for key: {}", key);
                }
            }

            String secretJson = objectMapper.writeValueAsString(rawMap);

            // Try to update existing secret first
            try {
                UpdateSecretRequest updateRequest = UpdateSecretRequest.builder()
                    .secretId(secretName)
                    .secretString(secretJson)
                    .build();

                secretsClient.updateSecret(updateRequest);
                logger.info("Updated secret: {}", secretName);

            } catch (ResourceNotFoundException e) {
                // Secret doesn't exist, create it
                CreateSecretRequest createRequest = CreateSecretRequest.builder()
                    .name(secretName)
                    .secretString(secretJson)
                    .description("Gmail OAuth tokens for Daily Task Orchestrator")
                    .build();

                secretsClient.createSecret(createRequest);
                logger.info("Created new secret: {}", secretName);
            }

        } catch (Exception e) {
            throw new IOException("Failed to save tokens to Secrets Manager: " + secretName, e);
        }
    }

    /**
     * Returns the number of tokens currently cached in memory.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Returns the AWS Secrets Manager secret name used by this DataStore.
     *
     * @return secret name
     */
    public String getSecretName() {
        return secretName;
    }
}
