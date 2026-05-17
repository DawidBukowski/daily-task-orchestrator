Tutaj łączymy klocki ze sobą. Jest całkowicie niezależny od tego, czy używamy Gmaila, czy Outlooka.
```java
public class DailyTaskOrchestrator {
    private final List<DataSource> dataSources;
    private final TaskAnalyzer analyzer;
    private final TaskNotifier notifier;

    public DailyTaskOrchestrator(List<DataSource> dataSources, TaskAnalyzer analyzer, TaskNotifier notifier) {
        this.dataSources = dataSources;
        this.analyzer = analyzer;
        this.notifier = notifier;
    }
    // ... metoda execute() ...
}
```
* **Dependency Injection (Wstrzykiwanie Zależności):** Zauważ, że konstruktor przyjmuje interfejsy jako parametry. Orchestrator sam z siebie _nie tworzy_ obiektów (nie ma tu słówka `new GmailDataSource()`). Obiekty są mu podawane (wstrzykiwane) z zewnątrz. Dzięki temu kod jest modularny i bardzo łatwy do testowania (możemy mu wstrzyknąć "fałszywe" adaptery do testów).
* `execute()`: Główny przepływ (Workflow).
* 1. Pętla `for (DataSource source : dataSources)` iteruje przez wszystkie podpięte źródła i pobiera surowe zadania.
* 2. `normalizeTasks()`: Zmienia dane surowe (`RawTask`) w ustandaryzowane (`Task`).
* 3. Wysyła zadania do analizy (AI).
* 4. Wysyła wynikowy raport powiadomieniem.
* `try-catch`: Blok łapiący wyjątki (błędy). Jeśli padnie serwer Gmaila, program nie "wybuchnie", tylko elegancko zapisze błąd przez `logger.error(...)`.