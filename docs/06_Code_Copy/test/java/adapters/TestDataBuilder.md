"Fabryka" testowych danych. Zamiast w każdym teście pisać od nowa obiektów z wymyślonymi nazwami, robimy to raz tutaj.
```java
public class TestDataBuilder {
    public static RawTask buildRawTask() {
        return new RawTask("Test Source", "Test Email", "Hello...", LocalDateTime.now());
    }

    public static Task buildTask() {
        return new Task("1", "Process Email", "Hello...", LocalDateTime.now().plusDays(1), "HIGH", "Test Source", "TODO");
    }
    // ... metoda buildAnalyzedTasks() ...
}
```
* **Wzorzec Projektowy (Test Data Builder / Object Mother):** Dzięki temu plikowi nasze testy stają się o wiele czytelniejsze. Jeśli kiedykolwiek dodamy nowe pole do klasy `Task` (np. przypisana kategoria), będziemy musieli zaktualizować konstruktor tylko w tym jednym miejscu, a nie w 50 plikach testowych.
* Wszystkie metody są `static`, więc wywołujemy je pisząc po prostu `TestDataBuilder.buildTask()`.
