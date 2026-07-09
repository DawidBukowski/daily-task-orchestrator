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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class GmailOAuth2Handler {
    private static final Logger logger = Logger.getLogger(GmailOAuth2Handler.class.getName());
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GmailConfiguration config;
    private final NetHttpTransport httpTransport;
    private final boolean isLambdaExecution;

    public GmailOAuth2Handler(GmailConfiguration config, NetHttpTransport httpTransport) {
        this.config = config;
        this.httpTransport = httpTransport;
        this.isLambdaExecution = "lambda".equalsIgnoreCase(System.getenv("DEPLOYMENT_ENV"));

        logger.info("GmailOAuth2Handler initialized in " +
                   (isLambdaExecution ? "LAMBDA" : "LOCAL") + " mode");
    }

    public Credential authenticate() {
        try {
            logger.info("Initializing OAuth2 flow in " +
                       (isLambdaExecution ? "Lambda" : "local") + " mode...");

            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
            details.setClientId(config.getClientId());
            details.setClientSecret(config.getClientSecret());

            GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
            clientSecrets.setInstalled(details);

            GoogleAuthorizationCodeFlow.Builder flowBuilder = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, config.getScopes())
                    .setAccessType("offline"); // Required to get a refresh token

            if (isLambdaExecution) {
                // Lambda: Use Secrets Manager for token storage (no interactive auth)
                logger.info("Using SecretsManagerDataStoreFactory for token storage");

                String awsRegion = System.getenv("AWS_REGION");
                if (awsRegion == null || awsRegion.isBlank()) {
                    throw new RuntimeException(
                        "AWS_REGION environment variable is required in Lambda mode"
                    );
                }

                SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

                flowBuilder.setDataStoreFactory(
                    new SecretsManagerDataStoreFactory(secretsClient, "daily-task-orchestrator")
                );

                GoogleAuthorizationCodeFlow flow = flowBuilder.build();

                // Tokens MUST already exist in Secrets Manager
                Credential credential = flow.loadCredential("user");
                if (credential == null) {
                    throw new RuntimeException(
                        "No OAuth tokens found in Secrets Manager. " +
                        "Initialize tokens locally first, then upload to AWS Secrets Manager."
                    );
                }

                logger.info("Successfully loaded OAuth credentials from Secrets Manager");

                // Refresh token if expired
                if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
                    logger.info("Token expired, refreshing...");
                    credential.refreshToken();
                }

                return credential;

            } else {
                // Local: Use FileDataStoreFactory for token storage (interactive auth OK)
                logger.info("Using FileDataStoreFactory for token storage");

                File tokenFolder = new File(config.getTokenDirectory());
                if (!tokenFolder.exists() && !tokenFolder.mkdirs()) {
                    logger.warning("Could not create token directory: " + tokenFolder.getAbsolutePath());
                }

                flowBuilder.setDataStoreFactory(new FileDataStoreFactory(tokenFolder));
                GoogleAuthorizationCodeFlow flow = flowBuilder.build();

                // Interactive browser-based OAuth flow
                LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
                Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

                if (credential.getRefreshToken() != null) {
                    logger.info("OAuth2 flow successful. Refresh token acquired and securely stored.");
                } else if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
                    logger.info("Token expired, refreshing...");
                    credential.refreshToken();
                }

                return credential;
            }

        } catch (IOException e) {
            logger.severe("Authentication failed due to network or IO error: " + e.getMessage());
            throw new RuntimeException("Failed to authenticate with Gmail API", e);
        }
    }
}