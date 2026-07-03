package com.dailytask.adapters.secrets;

import com.dailytask.core.ports.SecretsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SecretsProvider implementation that reads from environment variables.
 * Used for local development and backward compatibility.
 *
 * <p>This implementation provides zero-configuration secret access using
 * the operating system's environment variables via {@link System#getenv()}.
 *
 * <p>Thread-safe: This class is stateless and safe for concurrent use.
 */
public class EnvironmentSecretsProvider implements SecretsProvider {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentSecretsProvider.class);

    public EnvironmentSecretsProvider() {
        logger.info("Initialized environment variables secrets provider");
    }

    @Override
    public Optional<String> getSecret(String key) {
        if (key == null || key.isBlank()) {
            logger.debug("Attempted to retrieve secret with null or blank key");
            return Optional.empty();
        }

        String value = System.getenv(key);
        if (value != null) {
            logger.debug("Retrieved secret '{}' from environment", key);
        } else {
            logger.debug("Secret '{}' not found in environment", key);
        }
        return Optional.ofNullable(value);
    }

    @Override
    public Map<String, String> getAllSecrets() {
        // Return all environment variables
        // Note: In production, this should be filtered to application-relevant variables only
        Map<String, String> allEnvVars = new HashMap<>(System.getenv());
        logger.debug("Retrieved {} environment variables", allEnvVars.size());
        return allEnvVars;
    }

    @Override
    public Map<String, String> getStructuredSecret(String secretName) throws SecretsException {
        throw new SecretsException(
            "Structured secrets not supported in EnvironmentSecretsProvider. " +
            "Use individual environment variables instead, or switch to AwsSecretsManagerProvider."
        );
    }
}
