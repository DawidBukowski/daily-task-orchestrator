```java
@ExtendWith(MockitoExtension.class)
class DailyTaskOrchestratorTest {
    @Mock private DataSource mockDataSource;
    @Mock private TaskSummarizer mockSummarizer;
    // ...
    @Test
    void shouldExecuteFullWorkflow() {
        // Arrange
        when(mockDataSource.fetch(any(Instant.class))).thenReturn(testData);
        
        // Act
        orchestrator.execute();
        
        // Assert
        verify(mockDataSource, times(1)).fetch(any(Instant.class));
    }
}
```
* `@ExtendWith(MockitoExtension.class)`: Włącza bibliotekę Mockito do tego pliku testowego.
* `@Mock`: Tworzy tzw. "atrapy". Zamiast prawdziwego, powolnego API, Mockito generuje fałszywy obiekt, który zachowuje się tak, jak mu rozkażemy.
* Podejście **Arrange - Act - Assert** (AAA):
* **Arrange (Przygotuj):** Ustawiamy zachowanie atrap. `when(...).thenReturn(...)` mówi: "Kiedy ktoś wywoła metodę fetch(Instant), zwróć przygotowaną wcześniej listę mockRawData"
* **Act (Działaj):** Odpalamy prawdziwą metodę na naszym głównym obiekcie (`orchestrator.execute()`).
* **Assert (Sprawdź):** Weryfikujemy, czy aplikacja zadziałała poprawnie. `verify(..., times(1))` sprawdza układ nerwowy programu: "Czy na pewno podczas działania metody `execute()`, aplikacja odwołała się do metody `fetch(Instant)` dokładnie jeden raz?".
