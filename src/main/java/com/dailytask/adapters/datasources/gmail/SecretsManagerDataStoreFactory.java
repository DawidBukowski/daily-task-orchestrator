package com.dailytask.adapters.datasources.gmail;

import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.IOException;
import java.io.Serializable;

/**
 * DataStoreFactory implementation that stores OAuth tokens in AWS Secrets Manager.
 * Used in Lambda environment to persist Gmail OAuth credentials.
 *
 * <p>This implementation replaces FileDataStoreFactory for production deployments
 * where the filesystem is ephemeral (AWS Lambda /tmp is cleared between executions).
 *
 * <p>Thread-safe: All operations delegate to SecretsManagerDataStore which is thread-safe.
 */
public class SecretsManagerDataStoreFactory extends AbstractDataStoreFactory {
    private static final Logger logger = LoggerFactory.getLogger(SecretsManagerDataStoreFactory.class);

    private final SecretsManagerClient secretsClient;
    private final String secretPrefix;

    /**
     * Creates a new SecretsManagerDataStoreFactory.
     *
     * @param secretsClient AWS Secrets Manager client
     * @param secretPrefix  Prefix for secret names (e.g., "daily-task-orchestrator")
     * @throws IllegalArgumentException if secretsClient is null or secretPrefix is blank
     */
    public SecretsManagerDataStoreFactory(SecretsManagerClient secretsClient, String secretPrefix) {
        if (secretsClient == null) {
            throw new IllegalArgumentException("SecretsManagerClient must not be null");
        }
        if (secretPrefix == null || secretPrefix.isBlank()) {
            throw new IllegalArgumentException("Secret prefix must not be null or blank");
        }

        this.secretsClient = secretsClient;
        this.secretPrefix = secretPrefix;

        logger.info("Initialized SecretsManagerDataStoreFactory with prefix: {}", secretPrefix);
    }

    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
        logger.debug("Creating DataStore for id: {}", id);
        return new SecretsManagerDataStore<>(this, id, secretsClient, secretPrefix);
    }
}
