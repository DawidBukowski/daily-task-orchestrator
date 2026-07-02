## Test [[ClaudeResponseParserTest]] - Testy parsera odpowiedzi JSON

Klasa testowa sprawdzająca poprawność działania [[ClaudeResponseParser]]. Weryfikuje ona czy parser prawidłowo tłumaczy różnorodne formaty i stany danych JSON (w tym błędy składniowe i brakujące pola) na ustrukturyzowane rekordy Javy.

```java
class ClaudeResponseParserTest {

    private ClaudeResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ClaudeResponseParser();
    }

    @Test
    void parse_withValidCompleteJson_shouldParseAllFields() {
        String json = """
            {
              "summary": "You have 3 high-priority tasks",
              "schedule": "Focus on critical items first",
              "recommendations": [
                "Break down large tasks",
                "Set realistic deadlines"
              ],
              "taskUpdates": [
                {
                  "taskId": "task-123",
                  "priority": "CRITICAL",
                  "estimatedHours": 5.5,
                  "notes": "Urgent deadline"
                }
              ]
            }
            """;

        ClaudeTaskSummaryResponse response = parser.parse(json);

        assertNotNull(response);
        assertEquals("You have 3 high-priority tasks", response.summary());
        assertEquals("Focus on critical items first", response.schedule());
        assertEquals(2, response.recommendations().size());
        assertEquals(1, response.taskUpdates().size());
    }
}
```

### Analiza przypadków testowych

* **Domyślne wartości przy brakach (Defaulting)**: Test `parse_withMissingFields_shouldUseDefaults` sprawdza sytuację, gdy JSON zawiera tylko pole `summary`. Parser powinien automatycznie uzupełnić brakujące listy jako puste i przypisać domyślny tekst do harmonogramu (`"No schedule"`).
* **Normalizacja priorytetów (Normalization)**: Testy `parse_withInvalidPriority_shouldNormalizeToMedium` oraz `parse_withMixedCasePriorities_shouldNormalizeToUppercase` upewniają się, że:
  - Nieznany priorytet (np. `"INVALID"`) zostaje bezpiecznie zastąpiony przez `MEDIUM`.
  - Pisownia z małych liter (np. `"high"`) zostaje automatycznie poprawiona na wielkie litery (`"HIGH"`).
* **Filtrowanie wadliwych wpisów (Defensive filtering)**: Test `parse_shouldFilterOutInvalidTaskUpdatesButKeepValid` upewnia się, że jeśli tablica `taskUpdates` zawiera błędne wpisy (np. bez identyfikatora zadania `taskId`), zostaną one odrzucone, a wszystkie pozostałe, poprawne aktualizacje zostaną pomyślnie sparsowane.

### Pojęcia dla nowicjuszy

* **Normalizacja danych (Data Normalization)**: Proces ujednolicania formatu danych przed wprowadzeniem ich do logiki systemu. Przykładowo, konwersja słowa "high" na "HIGH" pozwala uniknąć błędów porównywania napisów w dalszych etapach działania programu.
* **Testowanie wartości brzegowych (Boundary/Edge Case Testing)**: Wyszukiwanie i sprawdzanie skrajnych wartości, na których program mógłby się zawiesić. Tutaj takimi wartościami są np. ujemne wartości liczbowe (`estimatedHours: -2.5`), pusty string `"   "`, wartość `null` czy gigantyczne rozmiary danych wejściowych.
