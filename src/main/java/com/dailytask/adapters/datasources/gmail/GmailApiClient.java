package com.dailytask.adapters.datasources.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GmailApiClient {
    private static final Logger logger = Logger.getLogger(GmailApiClient.class.getName());
    private static final String USER_ID = "me";
    private final Gmail gmailService;

    public GmailApiClient(GmailOAuth2Handler authHandler, NetHttpTransport transport) {
        Credential credential = authHandler.authenticate();
        this.gmailService = new Gmail.Builder(transport, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Daily Task Orchestrator")
                .build();
    }

    public List<Message> getEmails(String query, long maxResults) {
        try {
            logger.info("Fetching emails with query: [" + query + "]");
            ListMessagesResponse response = gmailService.users().messages().list(USER_ID)
                    .setQ(query)
                    .setMaxResults(maxResults)
                    .execute();

            List<Message> messages = new ArrayList<>();
            if (response.getMessages() != null) {
                for (Message msgRef : response.getMessages()) {
                    // Fetch full message details
                    Message fullMessage = gmailService.users().messages().get(USER_ID, msgRef.getId()).execute();
                    messages.add(fullMessage);
                }
            }
            logger.info("Successfully fetched " + messages.size() + " messages.");
            return messages;
        } catch (IOException e) {
            logger.severe("Failed to fetch emails from Gmail API. Check network and API limits.");
            throw new RuntimeException("Gmail API fetch error", e);
        }
    }

    // Ready for future use (e.g. marking parsed tasks as read)
    public void markAsRead(String messageId) {
        // Implementation for removing UNREAD label
    }
}