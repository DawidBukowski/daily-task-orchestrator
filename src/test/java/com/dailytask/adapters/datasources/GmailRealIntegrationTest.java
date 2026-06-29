package com.dailytask.adapters.datasources;

import com.dailytask.adapters.datasources.gmail.*;
import com.dailytask.core.config.AppConfig;
import com.dailytask.core.config.GmailConfiguration;
import com.dailytask.core.domain.RawData;
import com.dailytask.core.domain.Task;
import com.dailytask.core.ports.TaskExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual integration test for verifying the full Gmail API pipeline.
 *
 * Prerequisites:
 * 1. credentials.json file must exist in the project root
 * 2. ~/.dailytask/gmail_tokens directory must contain a valid StoredCredential
 * 3. A test email with subject "test-manualny" must exist in the Gmail inbox (sent within last 24 hours)
 *
 * This test is disabled by default to prevent automatic execution during builds.
 * To run manually: Remove @Disabled annotation or run this test explicitly from IDE.
 */
@Disabled("Manual integration test - requires real Gmail API access and manually sent email")
class GmailRealIntegrationTest {

    private GmailDataSource dataSource;
    private TaskExtractor taskExtractor;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Read credentials from credentials.json file
        ObjectMapper objectMapper = new ObjectMapper();
        File credentialsFile = new File("credentials.json");

        if (!credentialsFile.exists()) {
            throw new IllegalStateException("credentials.json file not found in project root");
        }

        JsonNode rootNode = objectMapper.readTree(credentialsFile);
        JsonNode installedNode = rootNode.get("installed");

        String clientId = installedNode.get("client_id").asText();
        String clientSecret = installedNode.get("client_secret").asText();

        // 2. Initialize Gmail configuration
        GmailConfiguration config = new GmailConfiguration(
                clientId,
                clientSecret,
                "http://localhost:8888/Callback",
                "~/.dailytask/gmail_tokens",
                List.of(
                        "https://www.googleapis.com/auth/gmail.readonly",
                        "https://www.googleapis.com/auth/gmail.modify"
                )
        );

        // 3. Initialize Gmail components (same as AppConfig.createDataSources())
        NetHttpTransport httpTransport = new NetHttpTransport();
        GmailOAuth2Handler authHandler = new GmailOAuth2Handler(config, httpTransport);
        GmailApiClient apiClient = new GmailApiClient(authHandler, httpTransport);

        // 4. Initialize email processing components
        List<String> taskKeywords = List.of("assignment", "deadline", "due", "project", "submit", "quiz", "exam", "homework", "final", "presentation");
        EmailFilter emailFilter = new EmailFilter(taskKeywords);
        GmailMessageParser messageParser = new GmailMessageParser();
        EmailToRawDataConverter rawDataConverter = new EmailToRawDataConverter();
        int queryLimit = 20;

        // 5. Create GmailDataSource
        this.dataSource = new GmailDataSource(apiClient, emailFilter, messageParser, rawDataConverter, queryLimit);

        // 6. Create TaskExtractor (same as AppConfig.createTaskExtractor())
        this.taskExtractor = AppConfig.createTaskExtractor();
    }

    /**
     * Test the full pipeline from Gmail API to Task objects.
     *
     * This test:
     * 1. Connects to real Gmail API
     * 2. Searches for email with subject "test-manualny" sent in last 24 hours
     * 3. Fetches and parses the email into RawData
     * 4. Extracts Task objects from RawData
     * 5. Validates the Task objects are correctly populated
     */
    @Test
    void testFetchAndProcessManualEmail() {
        // STEP 1: Fetch emails from Gmail API (last 24 hours)
        Instant from = Instant.now().minus(Duration.ofHours(24));
        List<RawData> rawDataList = dataSource.fetch(from);

        // STEP 2: Assert that at least one email was fetched
        assertNotNull(rawDataList, "RawData list should not be null");
        assertFalse(rawDataList.isEmpty(),
                "No emails found with task keywords in last 24 hours. Please send a test email with subject 'test-manualny' containing task keywords.");

        System.out.println("Fetched " + rawDataList.size() + " email(s) from Gmail API:");
        for (RawData rawData : rawDataList) {
            System.out.println("  - Title: " + rawData.getTitle());
            System.out.println("    Source: " + rawData.getSource());
            System.out.println("    Original ID: " + rawData.getOriginalSource());
            System.out.println("    Priority: " + rawData.getPriority());
        }

        // STEP 3: Run RawData through SimpleTaskExtractor ???? czemu idze przez interfejs , a nie faktyczną implementacje ???
        List<Task> tasks = taskExtractor.extract(rawDataList);

        // STEP 4: Assert Task extraction succeeded
        assertNotNull(tasks, "Task list should not be null");
        assertFalse(tasks.isEmpty(), "Task extraction should produce at least one Task");

        System.out.println("\nExtracted " + tasks.size() + " task(s):");

        // STEP 5: Validate each Task
        for (Task task : tasks) {
            System.out.println("  - Task ID: " + task.getId());
            System.out.println("    Title: " + task.getTitle());
            System.out.println("    Priority: " + task.getPriority());
            System.out.println("    Status: " + task.getStatus());
            System.out.println("    Source: " + task.getSource());
            System.out.println("    Deadline: " + task.getDeadline());
            System.out.println("    Created At: " + task.getCreatedAt());
            System.out.println();

            // Assertions
            assertNotNull(task.getId(), "Task ID should not be null");
            assertNotNull(task.getTitle(), "Task title should not be null");
            assertFalse(task.getTitle().isEmpty(), "Task title should not be empty");
            assertNotNull(task.getPriority(), "Task priority should not be null");
            assertNotNull(task.getStatus(), "Task status should not be null");
            assertEquals("Gmail", task.getSource(), "Task source should be 'Gmail'");
            assertNotNull(task.getOriginalId(), "Task originalId should not be null");
            assertNotNull(task.getCreatedAt(), "Task createdAt should not be null");
            assertNotNull(task.getUpdatedAt(), "Task updatedAt should not be null");
        }

        System.out.println("✅ Full pipeline test completed successfully!");
    }

    /**
     * Test that verifies the pipeline can handle a specific test email with subject "test-manualny".
     *
     * This test searches specifically for the test email to ensure the manual test email exists.
     */
    @Test
    void testSearchForSpecificTestEmail() {
        // Fetch emails from last 24 hours
        Instant from = Instant.now().minus(Duration.ofHours(24));
        List<RawData> rawDataList = dataSource.fetch(from);

        // Filter for emails containing "test-manualny" in title or content
        List<RawData> testEmails = rawDataList.stream()
                .filter(rawData ->
                        (rawData.getTitle() != null && rawData.getTitle().toLowerCase().contains("test-manualny")) ||
                        (rawData.getRawContent() != null && rawData.getRawContent().toLowerCase().contains("test-manualny"))
                )
                .toList();

        assertFalse(testEmails.isEmpty(),
                "No test email found with 'test-manualny' in subject or body. " +
                "Please send an email to yourself with subject containing 'test-manualny' and task keywords like 'assignment', 'deadline', or 'due'.");

        System.out.println("Found " + testEmails.size() + " test email(s) with 'test-manualny':");
        for (RawData testEmail : testEmails) {
            System.out.println("  - Title: " + testEmail.getTitle());
            System.out.println("    Priority: " + testEmail.getPriority());

            // Extract Task from test email
            List<Task> tasks = taskExtractor.extract(List.of(testEmail));
            assertFalse(tasks.isEmpty(), "Task extraction should succeed for test email");

            Task task = tasks.get(0);
            System.out.println("    Extracted Task Title: " + task.getTitle());
            System.out.println("    Extracted Task Priority: " + task.getPriority());
            System.out.println("    Extracted Task Deadline: " + task.getDeadline());
        }

        System.out.println("✅ Specific test email found and processed successfully!");
    }
}
