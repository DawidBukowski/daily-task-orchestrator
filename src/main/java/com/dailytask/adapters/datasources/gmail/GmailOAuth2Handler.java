package com.dailytask.adapters.datasources.gmail;

import com.dailytask.core.config.GmailConfiguration;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class GmailOAuth2Handler {
    private static final Logger logger = Logger.getLogger(GmailOAuth2Handler.class.getName());
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GmailConfiguration config;
    private final NetHttpTransport httpTransport;

    public GmailOAuth2Handler(GmailConfiguration config, NetHttpTransport httpTransport) {
        this.config = config;
        this.httpTransport = httpTransport;
    }

    public Credential authenticate() {
        try {
            logger.info("Initializing OAuth2 flow...");

            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
            details.setClientId(config.getClientId());
            details.setClientSecret(config.getClientSecret());

            GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
            clientSecrets.setWeb(details);

            File tokenFolder = new File(config.getTokenDirectory());
            if (!tokenFolder.exists() && !tokenFolder.mkdirs()) {
                logger.warning("Could not create token directory: " + tokenFolder.getAbsolutePath());
            }

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, config.getScopes())
                    .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
                    .setAccessType("offline") // Required to get a refresh token
                    .build();

            // Note: In production AWS, replace FileDataStoreFactory with a custom SecretsManagerDataStoreFactory.
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            if (credential.getRefreshToken() != null) {
                logger.info("OAuth2 flow successful. Refresh token acquired and securely stored.");
            } else if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
                logger.info("Token expired, refreshing...");
                credential.refreshToken();
            }

            return credential;
        } catch (IOException e) {
            logger.severe("Authentication failed due to network or IO error.");
            throw new RuntimeException("Failed to authenticate with Gmail API", e);
        }
    }
}