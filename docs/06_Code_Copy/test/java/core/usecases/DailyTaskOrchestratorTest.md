```java
@ExtendWith(MockitoExtension.class)
class DailyTaskOrchestratorTest {
    @Mock private DataSource mockDataSource;
    @Mock private TaskAnalyzer mockAnalyzer;
    // ...
    @Test
    void shouldExecuteFullWorkflow() {
        // Arrange
        when(mockDataSource.fetch()).thenReturn(mockRawTasks);
        
        // Act
        orchestrator.execute();
        
        // Assert
        verify(mockDataSource, times(1)).fetch();
    }
}
```
* `@ExtendWith(MockitoExtension.class)`: Włącza bibliotekę Mockito do tego pliku testowego.
* `@Mock`: Tworzy tzw. "atrapy". Zamiast prawdziwego, powolnego API, Mockito generuje fałszywy obiekt, który zachowuje się tak, jak mu rozkażemy.
* Podejście **Arrange - Act - Assert** (AAA):
* **Arrange (Przygotuj):** Ustawiamy zachowanie atrap. `when(...).thenReturn(...)` mówi: "Kiedy ktoś wywoła metodę fetch(), zwróć przygotowaną wcześniej listę mockRawTasks"
* **Act (Działaj):** Odpalamy prawdziwą metodę na naszym głównym obiekcie (`orchestrator.execute()`).
* **Assert (Sprawdź):** Weryfikujemy, czy aplikacja zadziałała poprawnie. `verify(..., times(1))` sprawdza układ nerwowy programu: "Czy na pewno podczas działania metody `execute()`, aplikacja odwołała się do metody `fetch()` dokładnie jeden raz?".