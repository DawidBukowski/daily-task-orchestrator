## Test [[TaskSummarizationPromptBuilderTest]] - Testy kreatora promptów

Klasa testowa sprawdzająca poprawność generowania promptów przez [[TaskSummarizationPromptBuilder]]. Weryfikuje strukturę instrukcji systemowych (w tym poprawność schematu JSON) oraz właściwe formatowanie informacji o zadaniach.

```java
class TaskSummarizationPromptBuilderTest {

    private TaskSummarizationPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new TaskSummarizationPromptBuilder();
    }

    @Test
    void buildSystemPrompt_shouldDefineJsonSchema() {
        String systemPrompt = builder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("\"summary\""));
        assertTrue(systemPrompt.contains("\"schedule\""));
        assertTrue(systemPrompt.contains("\"recommendations\""));
        assertTrue(systemPrompt.contains("\"taskUpdates\""));
    }

    @Test
    void buildUserPrompt_withSingleTask_shouldIncludeAllFields() {
        LocalDate today = LocalDate.now();
        Task task = new Task(
                "task-123",
                "Implement feature X",
                "Detailed description here",
                LocalDateTime.now().plusDays(2),
                Priority.HIGH,
                "gmail",
                "orig-456",
                TaskStatus.PENDING,
                3.5,
                List.of("urgent", "backend"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Some notes"
        );

        String userPrompt = builder.buildUserPrompt(List.of(task), today);

        assertTrue(userPrompt.contains("task-123"));
        assertTrue(userPrompt.contains("Implement feature X"));
        assertTrue(userPrompt.contains("HIGH"));
        assertTrue(userPrompt.contains("3.5"));
    }
}
```

### Analiza scenariuszy testowych

* **Weryfikacja schematu JSON**: Ponieważ API Claude wymaga ściśle określonego formatu wyjściowego, testy sprawdzają, czy prompt systemowy zawiera wszystkie niezbędne klucze JSON (np. `"summary"`, `"taskUpdates"`), zapobiegając przypadkowemu usunięciu kluczowych reguł podczas edycji promptów.
* **Ignorowanie pustych sekcji**: Test `buildUserPrompt_withTaskWithoutDescription_shouldNotIncludeDescriptionSection` i pokrewne sprawdzają, czy w przypadku braku opcjonalnych danych (np. opisu czy deadline-u) generator nie wstawia pustych linii ani napisów typu `"Description: null"`.
* **Walidacja formatu daty (Regex)**: Test `buildUserPrompt_currentDateFormat_shouldIncludeDayOfWeek` upewnia się, że przekazana data jest sformatowana zgodnie z maską zawierającą nazwę dnia tygodnia w nawiasie okrągłym.

### Pojęcia dla nowicjuszy

* **Wyrażenia Regularne (Regular Expressions / Regex)**: Specjalne wzorce tekstowe służące do wyszukiwania lub dopasowywania tekstu. W teście zastosowano wyrażenie `"(?s).*2026-06-30\\s*\\([A-Za-z]+\\).*"` w celu sprawdzenia, czy wygenerowany prompt zawiera datę oraz w nawiasie dowolne słowo (nazwę dnia tygodnia), niezależnie od języka systemu (np. "Tuesday" lub "wtorek").
* **Asercje tekstowe (`contains`)**: Metody sprawdzające, czy dany fragment tekstu (podciąg) znajduje się wewnątrz dłuższego napisu. Jest to prosty i czytelny sposób testowania dynamicznie generowanych tekstów.
