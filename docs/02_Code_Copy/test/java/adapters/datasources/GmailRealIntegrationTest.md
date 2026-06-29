Manualny test integracyjny weryfikujący pełną integrację z zewnętrznym API Gmaila (OAuth2) oraz poprawne przejście przez cały potok przetwarzania danych.

```java
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
}
```

* `@Disabled`: Test jest wyłączony z automatycznego uruchamiania (np. podczas `mvn test` w procesie CI/CD), ponieważ do poprawnego działania wymaga:
  1. Istnienia pliku `credentials.json` w głównym katalogu projektu.
  2. Zalogowanego konta w systemie i zapisanego tokenu OAuth2 w lokalnym katalogu `~/.dailytask/gmail_tokens`.
  3. Wysłania na powiązane konto testowej wiadomości e-mail w ciągu ostatnich 24 godzin zawierającej frazy kluczowe (np. w temacie `test-manualny`).
* `setUp()`: Ręcznie buduje graf zależności aplikacji — odczytuje plik z poświadczeniami Google, konfiguruje i tworzy [[GmailApiClient]] wraz z mechanizmem uwierzytelniania, oraz inicjuje [[GmailDataSource]] z zestawem słów kluczowych do filtrowania zadań.
* **Dlaczego używamy interfejsu `TaskExtractor` zamiast konkretnej klasy?**
  - W kroku `this.taskExtractor = AppConfig.createTaskExtractor()` przypisujemy obiekt do interfejsu [[TaskExtractor]] (którego faktyczną implementacją w tym przypadku jest [[SimpleTaskExtractor]]).
  - Użycie interfejsu pozwala zachować niskie sprzężenie kodu (low coupling). Jeśli w przyszłości zmienimy ekstraktor zadań na bardziej zaawansowany (np. oparty o sztuczną inteligencję Claude — [[ClaudeRawDataAnalyzer]]), kod testu integracyjnego pozostanie bez zmian. Wystarczy, że nowa klasa będzie implementować kontrakt interfejsu [[TaskExtractor]].
* Testy weryfikują m.in.:
  - Pobieranie wiadomości z serwerów Google (`testFetchAndProcessManualEmail`).
  - Poprawność filtrowania i ekstrakcji dedykowanego e-maila testowego z tematem zawierającym słowo `test-manualny` (`testSearchForSpecificTestEmail`).
