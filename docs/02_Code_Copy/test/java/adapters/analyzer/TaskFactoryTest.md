Testy jednostkowe weryfikujące poprawność fabryki zadań [[TaskFactory]], odpowiedzialnej za konwersję surowych danych [[RawData]] na kompletne obiekty domenowe [[Task]].

```java
class TaskFactoryTest {

    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
    }

    @Test
    void testCreateFromRawData_completeData() {
        RawData rawData = new RawData(
            "Gmail",
            "Assignment 1",
            "Complete homework. Due Friday",
            LocalDateTime.of(2026, 6, 29, 10, 0),
            "sender@example.com",
            "msg-123",
            "HIGH",
            Collections.emptyMap()
        );

        Task task = factory.createFromRawData(rawData);

        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("Assignment 1", task.getTitle());
        assertEquals("Complete homework. Due Friday", task.getDescription());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-123", task.getOriginalId());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
        assertNotNull(task.getDeadline());
    }
}
```

* `testCreateFromRawData_completeData()`: Sprawdza podstawowy przypadek użycia fabryki. Weryfikuje, czy wszystkie kluczowe atrybuty (takie jak priorytet, źródło, opis, oryginalny ID) są poprawnie przenoszone z [[RawData]] do [[Task]] oraz czy generowane są daty utworzenia/modyfikacji.
* Klasa testuje również mechanizmy rezerwowe (fallbacks) i reguły biznesowe:
    - **Ekstrakcja terminu**: Czy termin wykonania jest automatycznie wyciągany z treści przy użyciu parsera.
    - **Obsługa priorytetów**: Testuje odporność na wielkość liter (np. „low”) i nieznane wartości (domyślny priorytet `MEDIUM`).
    - **Wartości domyślne**: Sprawdza zachowanie dla pól, które mogą być puste (brak priorytetu skutkuje przypisaniem `MEDIUM`, status domyślnie to `PENDING`).
    - **Walidacja**: Weryfikuje, czy przekazanie `null` do fabryki rzuca oczekiwany wyjątek `IllegalArgumentException`.
    - **Unikalność ID**: Potwierdza, że każde nowo utworzone zadanie otrzymuje unikalny identyfikator UUID (nawet przy takich samych danych wejściowych).
