package com.dailytask.core.ports;

import java.util.Map;
import java.util.Optional;

/**
 * Port interface for retrieving application secrets.
 *
 * <p>Abstracts secrets retrieval to support multiple backends:
 * <ul>
 *   <li>Environment variables (development)</li>
 *   <li>AWS Secrets Manager (production)</li>
 *   <li>Other secret stores (future)</li>
 * </ul>
 *
 * <p>Implementations should be thread-safe as they may be called concurrently
 * during application initialization.
 */
public interface SecretsProvider {

    /**
     * Retrieves a single secret value by key.
     *
     * @param key the secret key to retrieve
     * @return Optional containing the secret value if found, empty otherwise
     */
    Optional<String> getSecret(String key);

    /**
     * Retrieves all secrets as a key-value map.
     * Useful for bulk loading configuration.
     *
     * @return Map of all available secrets (never null, may be empty)
     */
    Map<String, String> getAllSecrets();

    /**
     * Retrieves a structured secret (JSON) as a Map.
     * Used for loading multiple configuration values from a single secret.
     *
     * @param secretName the name of the secret in the secrets store
     * @return Map representation of the JSON secret (never null)
     * @throws SecretsException if secret cannot be retrieved or parsed
     */
    Map<String, String> getStructuredSecret(String secretName) throws SecretsException;

    /**
     * Exception thrown when secret retrieval fails.
     */
    class SecretsException extends Exception {
        public SecretsException(String message) {
            super(message);
        }

        public SecretsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
