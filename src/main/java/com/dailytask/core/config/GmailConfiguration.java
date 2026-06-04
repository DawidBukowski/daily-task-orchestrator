package com.dailytask.core.config;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.Objects;

public class GmailConfiguration {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenDirectory;
    private final List<String> scopes;

    public GmailConfiguration(String clientId, String clientSecret, String redirectUri, String tokenDirectory, List<String> scopes) {
        this.clientId = Objects.requireNonNull(clientId, "Client ID cannot be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "Client Secret cannot be null");
        this.redirectUri = Objects.requireNonNull(redirectUri, "Redirect URI cannot be null");
        this.tokenDirectory = Objects.requireNonNull(tokenDirectory, "Token directory cannot be null");
        this.scopes = Objects.requireNonNull(scopes, "Scopes cannot be null");

        if (this.clientId.isBlank() || this.clientSecret.isBlank()) {
            throw new IllegalStateException("Gmail credentials (client-id or client-secret) are missing or blank. Please check your environment variables or configuration file.");
        }
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public String getTokenDirectory() {
        if (tokenDirectory.startsWith("~/")) {
            return System.getProperty("user.home") + tokenDirectory.substring(1);
        } else if (tokenDirectory.startsWith("~")) {
            return System.getProperty("user.home") + File.separator + tokenDirectory.substring(1);
        }
        return tokenDirectory;
    }
    public List<String> getScopes() { return scopes; }
}