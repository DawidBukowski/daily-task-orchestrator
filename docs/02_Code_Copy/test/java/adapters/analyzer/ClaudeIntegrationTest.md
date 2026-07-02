## Test [[ClaudeIntegrationTest]] - Testy integracyjne potoku Claude

Klasa testowa realizująca testy integracyjne całego potoku analizy zadań przez Claude AI. Testuje współpracę klas [[ClaudeConfiguration]], [[TaskSummarizationPromptBuilder]], [[ClaudeResponseParser]] oraz [[ClaudeTasksSummarizer]].

```java
@ExtendWith(MockitoExtension.class)
class ClaudeIntegrationTest {

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
    void fullPipeline_shouldProcessTasksFromConfigurationThroughSummarization() throws Exception {
        // 1. Przygotowanie zadań wejściowych
        Task task1 = createTask("task-001", "Implement login feature", Priority.CRITICAL, 8.0);
        // ...
        
        // 2. Mockowanie odpowiedzi JSON z API Claude
        String apiResponse = "{ ... }";
        when(mockApiClient.sendMessage(any(), any())).thenReturn(apiResponse);

        // 3. Uruchomienie potoku
        TasksSummary result = summarizer.summarize(List.of(task1, task2, task3));

        // 4. Weryfikacja efektów
        assertNotNull(result);
        assertEquals(3, result.getAllTasks().size());
        // ...
    }
}
```

### Cel i zakres testów integracyjnych

* **Integracja komponentów (End-to-End)**: W przeciwieństwie do testów jednostkowych, które sprawdzają każdą klasę w całkowitej izolacji, test integracyjny sprawdza, czy klasy współgrają ze sobą prawidłowo. Przechodzimy pełną drogę: od surowego obiektu [[Task]], przez wygenerowanie promptu tekstowego, jego przekazanie do (zamockowanego) klienta API, aż po sparsowanie odpowiedzi i zwrócenie obiektu [[TasksSummary]].
* **Weryfikacja niezmienności (Immutability)**: Test `fullPipeline_shouldPreserveTaskImmutability` upewnia się, że proces aktualizacji priorytetów lub czasów zadań nie modyfikuje oryginalnych obiektów `Task` (które powinny być niemodyfikowalne dla bezpieczeństwa wątkowego), lecz tworzy poprawne nowe instancje lub ich reprezentacje.
* **Odporność na błędy (Fault Tolerance)**: Scenariusze takie jak `fullPipeline_withApiFailure_shouldReturnFallbackSummary` weryfikują zachowanie systemu w przypadku awarii sieci lub otrzymania zniekształconego JSON-a, sprawdzając, czy aplikacja potrafi bezpiecznie powrócić do stabilnego stanu (graceful degradation).

### Pojęcia dla nowicjuszy

* **Testy integracyjne (Integration Tests)**: Testy sprawdzające interakcję i poprawne działanie kilku współpracujących ze sobą modułów lub klas systemu jednocześnie.
* **Immutability (Niezmienność)**: Projektowanie obiektów w taki sposób, aby ich stanu nie dało się zmienić po utworzeniu. Zamiast modyfikacji pól, tworzy się nową kopię obiektu z nowymi wartościami. Pomaga to uniknąć trudnych do zdiagnozowania błędów synchronizacji.
* **Pipeline (Potok)**: Sekwencja przetwarzania danych, gdzie wyjście jednego kroku (np. wygenerowanie promptu) staje się wejściem kolejnego kroku (wysłanie do API), aż do uzyskania ostatecznego rezultatu.
