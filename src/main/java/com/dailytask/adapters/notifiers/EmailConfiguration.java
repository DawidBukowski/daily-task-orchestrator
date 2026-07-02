package com.dailytask.adapters.notifiers;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Configuration for email notification via SMTP.
 * Loads and validates SMTP settings from environment variables.
 */
public class EmailConfiguration {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String smtpHost;
    private final int smtpPort;
    private final String username;
    private final String password;
    private final String fromEmail;
    private final String toEmail;
    private final boolean enableTls;
    private final boolean enableAuth;
    private final int timeoutMs;

    private EmailConfiguration(Builder builder) {
        this.smtpHost = Objects.requireNonNull(builder.smtpHost, "SMTP host cannot be null");
        this.smtpPort = builder.smtpPort;
        this.username = Objects.requireNonNull(builder.username, "Username cannot be null");
        this.password = Objects.requireNonNull(builder.password, "Password cannot be null");
        this.fromEmail = Objects.requireNonNull(builder.fromEmail, "From email cannot be null");
        this.toEmail = Objects.requireNonNull(builder.toEmail, "To email cannot be null");
        this.enableTls = builder.enableTls;
        this.enableAuth = builder.enableAuth;
        this.timeoutMs = builder.timeoutMs;

        validate();
    }

    private void validate() {
        if (smtpHost.isBlank()) {
            throw new IllegalStateException("EMAIL_SMTP_HOST cannot be blank");
        }

        if (smtpPort < 1 || smtpPort > 65535) {
            throw new IllegalStateException(
                "EMAIL_SMTP_PORT must be between 1 and 65535, got: " + smtpPort
            );
        }

        if (username.isBlank()) {
            throw new IllegalStateException("EMAIL_USERNAME cannot be blank");
        }

        if (password.isBlank()) {
            throw new IllegalStateException("EMAIL_PASSWORD cannot be blank");
        }

        if (!EMAIL_PATTERN.matcher(fromEmail).matches()) {
            throw new IllegalStateException(
                "EMAIL_FROM must be a valid email address, got: " + fromEmail
            );
        }

        if (!EMAIL_PATTERN.matcher(toEmail).matches()) {
            throw new IllegalStateException(
                "EMAIL_TO must be a valid email address, got: " + toEmail
            );
        }

        if (timeoutMs <= 0) {
            throw new IllegalStateException(
                "EMAIL_TIMEOUT_MS must be positive, got: " + timeoutMs
            );
        }
    }

    /**
     * Creates EmailConfiguration from environment variables.
     *
     * Required environment variables:
     * - EMAIL_SMTP_HOST: SMTP server hostname (e.g., smtp.gmail.com)
     * - EMAIL_SMTP_PORT: SMTP server port (e.g., 587 for STARTTLS)
     * - EMAIL_USERNAME: SMTP authentication username
     * - EMAIL_PASSWORD: SMTP authentication password (app-specific password for Gmail)
     * - EMAIL_FROM: Sender email address
     * - EMAIL_TO: Recipient email address
     *
     * Optional environment variables:
     * - EMAIL_ENABLE_TLS: Enable STARTTLS (default: true)
     * - EMAIL_ENABLE_AUTH: Enable SMTP authentication (default: true)
     * - EMAIL_TIMEOUT_MS: Timeout in milliseconds (default: 30000)
     */
    public static EmailConfiguration fromEnv() {
        String smtpHost = System.getenv("EMAIL_SMTP_HOST");
        if (smtpHost == null || smtpHost.isBlank()) {
            throw new IllegalStateException("EMAIL_SMTP_HOST environment variable is required");
        }

        String smtpPortStr = System.getenv("EMAIL_SMTP_PORT");
        if (smtpPortStr == null || smtpPortStr.isBlank()) {
            throw new IllegalStateException("EMAIL_SMTP_PORT environment variable is required");
        }

        String username = System.getenv("EMAIL_USERNAME");
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("EMAIL_USERNAME environment variable is required");
        }

        String password = System.getenv("EMAIL_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("EMAIL_PASSWORD environment variable is required");
        }

        String fromEmail = System.getenv("EMAIL_FROM");
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("EMAIL_FROM environment variable is required");
        }

        String toEmail = System.getenv("EMAIL_TO");
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalStateException("EMAIL_TO environment variable is required");
        }

        return new Builder()
            .smtpHost(smtpHost)
            .smtpPort(getEnvInt("EMAIL_SMTP_PORT", smtpPortStr))
            .username(username)
            .password(password)
            .fromEmail(fromEmail)
            .toEmail(toEmail)
            .enableTls(getEnvBoolean("EMAIL_ENABLE_TLS", true))
            .enableAuth(getEnvBoolean("EMAIL_ENABLE_AUTH", true))
            .timeoutMs(getEnvInt("EMAIL_TIMEOUT_MS", 30000))
            .build();
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

    private static int getEnvInt(String key, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "Invalid integer value for " + key + ": " + value
            );
        }
    }

    private static boolean getEnvBoolean(String key, boolean defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    // Getters
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFromEmail() { return fromEmail; }
    public String getToEmail() { return toEmail; }
    public boolean isEnableTls() { return enableTls; }
    public boolean isEnableAuth() { return enableAuth; }
    public int getTimeoutMs() { return timeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String smtpHost;
        private int smtpPort = 587;
        private String username;
        private String password;
        private String fromEmail;
        private String toEmail;
        private boolean enableTls = true;
        private boolean enableAuth = true;
        private int timeoutMs = 30000;

        public Builder smtpHost(String smtpHost) {
            this.smtpHost = smtpHost;
            return this;
        }

        public Builder smtpPort(int smtpPort) {
            this.smtpPort = smtpPort;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder fromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
            return this;
        }

        public Builder toEmail(String toEmail) {
            this.toEmail = toEmail;
            return this;
        }

        public Builder enableTls(boolean enableTls) {
            this.enableTls = enableTls;
            return this;
        }

        public Builder enableAuth(boolean enableAuth) {
            this.enableAuth = enableAuth;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public EmailConfiguration build() {
            return new EmailConfiguration(this);
        }
    }
}
