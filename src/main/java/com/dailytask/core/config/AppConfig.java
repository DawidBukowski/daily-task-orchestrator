package com.dailytask.core.config;

import com.dailytask.adapters.analyzers.ClaudeTasksSummarizer;
import com.dailytask.adapters.analyzers.SimpleTaskExtractor;
import com.dailytask.adapters.datasources.GmailDataSource;
import com.dailytask.adapters.datasources.gmail.EmailFilter;
import com.dailytask.adapters.datasources.gmail.EmailToRawDataConverter;
import com.dailytask.adapters.datasources.gmail.GmailMessageParser;
import com.dailytask.adapters.notifiers.EmailTaskNotifier;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskExtractor;
import com.dailytask.core.ports.TaskSummarizer;
import com.dailytask.core.ports.TaskNotifier;
import com.dailytask.adapters.datasources.gmail.GmailApiClient;
import com.dailytask.adapters.datasources.gmail.GmailOAuth2Handler;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.util.List;

public class AppConfig {

    public static List<DataSource> createDataSources() {
        // 1. Pobierz zmienne środowiskowe z uprawnieniami do API Google
        String clientId = System.getenv("GMAIL_CLIENT_ID");
        String clientSecret = System.getenv("GMAIL_CLIENT_SECRET");

        // Zabezpieczenie na wypadek braku zmiennych
        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException("Brak zmiennych środowiskowych GMAIL_CLIENT_ID lub GMAIL_CLIENT_SECRET!");
        }

        // 2. Zbuduj konfigurację
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

        // 3. Zainicjalizuj komponenty Google API
        NetHttpTransport httpTransport = new NetHttpTransport();
        GmailOAuth2Handler authHandler = new GmailOAuth2Handler(config, httpTransport);
        GmailApiClient apiClient = new GmailApiClient(authHandler, httpTransport);

        // 4. Zainicjalizuj nowe komponenty do parsowania i filtrowania maili
        List<String> taskKeywords = List.of("assignment", "deadline", "due", "project", "submit", "quiz", "exam", "homework", "final", "presentation");
        EmailFilter emailFilter = new EmailFilter(taskKeywords);
        GmailMessageParser messageParser = new GmailMessageParser();
        EmailToRawDataConverter rawDataConverter = new EmailToRawDataConverter();
        int queryLimit = 20;

        // 5. Wstrzyknij wszystkie 5 zależności do adaptera i go zwróć
        return List.of(new GmailDataSource(apiClient, emailFilter, messageParser, rawDataConverter, queryLimit));
    }

    public static TaskExtractor createTaskExtractor() {
        return new SimpleTaskExtractor();
    }

    public static TaskSummarizer createAnalyzer() {
        return new ClaudeTasksSummarizer();
    }

    public static TaskNotifier createNotifier() {
        return new EmailTaskNotifier();
    }
}