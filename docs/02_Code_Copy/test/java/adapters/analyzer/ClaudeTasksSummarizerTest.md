## Test [[ClaudeTasksSummarizerTest]] - Testy sumaryzatora zadań Claude

Klasa testowa sprawdzająca poprawność działania sumaryzatora zadań [[ClaudeTasksSummarizer]]. Wykorzystuje Mockito do weryfikacji poprawnego delegowania zadań do interfejsu [[ClaudeApiClient]] oraz poprawnego scalania informacji.

```java
@ExtendWith(MockitoExtension.class)
class ClaudeTasksSummarizerTest {

    @Mock
    private ClaudeApiClient mockApiClient;

    private TaskSummarizationPromptBuilder promptBuilder;
    private ClaudeResponseParser responseParser;
    private ClaudeTasksSummarizer summarizer;

    @BeforeEach
    void setUp() {
        promptBuilder = new TaskSummarizationPromptBuilder();
        responseParser = new ClaudeResponseParser();
        summarizer = new ClaudeTasksSummarizer(mockApiClient, promptBuilder, responseParser);
    }

    @Test
    void summarize_withSuccessfulResponse_shouldApplyUpdates() throws ClaudeApiClient.ClaudeApiException {
        Task task1 = createTask("task-1", "First task", Priority.LOW, null);
        Task task2 = createTask("task-2", "Second task", Priority.MEDIUM, 2.0);

        String mockResponse = """
            {
              "summary": "Updated priorities",
              "schedule": "Focus on task-1 first",
              "recommendations": ["Prioritize task-1"],
              "taskUpdates": [
                {
                  "taskId": "task-1",
                  "priority": "CRITICAL",
                  "estimatedHours": 5.0,
                  "notes": "Urgent deadline approaching"
                }
              ]
            }
            """;

        when(mockApiClient.sendMessage(anyString(), anyString())).thenReturn(mockResponse);

        TasksSummary result = summarizer.summarize(List.of(task1, task2));

        assertNotNull(result);
        assertEquals("Updated priorities", result.getSummary());
        assertEquals(2, result.getAllTasks().size());

        Task updatedTask1 = findTaskById(result, "task-1");
        assertEquals(Priority.CRITICAL, updatedTask1.getPriority());
        assertEquals(5.0, updatedTask1.getEstimatedHours());
    }
}
```

### Analiza technik i scenariuszy testowych

* **Weryfikacja parametrów wejściowych (Defensive constructors)**: Pierwsze testy (`constructor_withNullApiClient_shouldThrowException` itp.) upewniają się, że klasa rzuca wyjątek `IllegalArgumentException`, jeśli którykolwiek z wymaganych obiektów w konstruktorze lub metodzie `summarize` jest równy `null`.
* **Scalanie notatek (Notes Merging)**: Metoda `summarize_shouldMergeNotesCorrectly` testuje kluczową funkcjonalność biznesową — scalanie dotychczasowych notatek użytkownika z nowo wygenerowaną sugestią od sztucznej inteligencji. Test upewnia się, że pierwotna treść notatki nie została skasowana, a dopisek od Claude został czytelnie oznaczony nagłówkiem `[AI Analysis]`.
* **Weryfikacja wywołań z filtrem parametrów (`argThat`)**: W teście `summarize_shouldCallApiWithCorrectPrompts` używamy zaawansowanego mechanizmu Mockito `argThat(...)` do sprawdzania, czy wygenerowane i przekazane do API prompty tekstowe zawierają kluczowe słowa i poprawne identyfikatory zadań.

### Pojęcia dla nowicjuszy

* **`argThat` (Dopasowanie argumentów)**: Zaawansowana funkcja Mockito pozwalająca na weryfikację argumentów przekazanych do metody za pomocą wyrażeń logicznych (np. sprawdzenie, czy przekazany tekst zawiera określone słowo), zamiast porównywania całego napisu jeden do jednego.
* **Merging (Scalanie)**: Proces łączenia dwóch zestawów danych lub napisów w jeden. W tym systemie program musi zadbać, aby dodanie analizy AI nie nadpisało (nie skasowało) notatek, które użytkownik sam wcześniej ręcznie wprowadził do zadania.
