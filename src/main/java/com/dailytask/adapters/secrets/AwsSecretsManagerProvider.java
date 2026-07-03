package com.dailytask.adapters.secrets;

import com.dailytask.core.ports.SecretsProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SecretsProvider implementation using AWS Secrets Manager.
 * Caches secrets in memory for the lifetime of the Lambda execution context.
 *
 * <p>This implementation optimizes for Lambda cold start performance and cost:
 * <ul>
 *   <li>Secrets are cached in memory to reduce API calls</li>
 *   <li>Cache is thread-safe using ConcurrentHashMap</li>
 *   <li>Cache lifetime equals Lambda execution context lifetime (~5-15 minutes)</li>
 * </ul>
 *
 * <p>Thread-safe: All methods use synchronized cache access.
 */
public class AwsSecretsManagerProvider implements SecretsProvider {
    private static final Logger logger = LoggerFactory.getLogger(AwsSecretsManagerProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SecretsManagerClient secretsClient;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final String region;

    /**
     * Creates a new AWS Secrets Manager provider for the specified region.
     *
     * @param region AWS region (e.g., "us-east-1")
     * @throws IllegalArgumentException if region is null or blank
     */
    public AwsSecretsManagerProvider(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS region must not be null or blank");
        }

        this.region = region;
        this.secretsClient = SecretsManagerClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

        logger.info("Initialized AWS Secrets Manager provider in region: {}", region);
    }

    /**
     * Constructor for testing with custom SecretsManagerClient.
     *
     * @param secretsClient custom Secrets Manager client (for mocking)
     * @param region AWS region
     */
    AwsSecretsManagerProvider(SecretsManagerClient secretsClient, String region) {
        if (secretsClient == null) {
            throw new IllegalArgumentException("SecretsManagerClient must not be null");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS region must not be null or blank");
        }

        this.secretsClient = secretsClient;
        this.region = region;
        logger.info("Initialized AWS Secrets Manager provider with custom client in region: {}", region);
    }

    @Override
    public Optional<String> getSecret(String key) {
        if (key == null || key.isBlank()) {
            logger.debug("Attempted to retrieve secret with null or blank key");
            return Optional.empty();
        }

        // Check cache first
        if (cache.containsKey(key)) {
            logger.debug("Retrieved secret '{}' from cache", key);
            return Optional.of(cache.get(key));
        }

        // Attempt to retrieve from Secrets Manager
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(key)
                .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretValue = response.secretString();

            cache.put(key, secretValue);
            logger.debug("Retrieved and cached secret '{}'", key);
            return Optional.of(secretValue);

        } catch (ResourceNotFoundException e) {
            logger.debug("Secret '{}' not found in Secrets Manager", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to retrieve secret '{}' from Secrets Manager: {}",
                       key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getAllSecrets() {
        return new HashMap<>(cache);
    }

    @Override
    public Map<String, String> getStructuredSecret(String secretName) throws SecretsException {
        if (secretName == null || secretName.isBlank()) {
            throw new SecretsException("Secret name must not be null or blank");
        }

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretJson = response.secretString();

            if (secretJson == null || secretJson.isBlank()) {
                throw new SecretsException("Secret '" + secretName + "' has empty value");
            }

            Map<String, String> secretMap = objectMapper.readValue(
                secretJson,
                new TypeReference<Map<String, String>>() {}
            );

            // Cache individual keys for future getSecret() calls
            cache.putAll(secretMap);
            logger.info("Retrieved and cached structured secret '{}' with {} keys",
                       secretName, secretMap.size());

            return secretMap;

        } catch (ResourceNotFoundException e) {
            throw new SecretsException(
                "Secret '" + secretName + "' not found in Secrets Manager", e
            );
        } catch (Exception e) {
            throw new SecretsException(
                "Failed to retrieve or parse structured secret '" + secretName + "': " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Closes the Secrets Manager client and releases resources.
     * Should be called when the provider is no longer needed.
     *
     * <p>Note: In Lambda, this is typically not needed as the execution context
     * is recycled automatically. However, it's good practice for local testing.
     */
    public void close() {
        if (secretsClient != null) {
            secretsClient.close();
            logger.debug("Closed Secrets Manager client");
        }
    }

    /**
     * Returns the AWS region this provider is configured for.
     *
     * @return AWS region string (e.g., "us-east-1")
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the number of secrets currently cached in memory.
     * Useful for monitoring and debugging.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
}
