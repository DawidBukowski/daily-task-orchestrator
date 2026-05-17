```java
public static void main(String[] args) {
    List<DataSource> sources = AppConfig.createDataSources();
    TaskAnalyzer analyzer = AppConfig.createAnalyzer();
    TaskNotifier notifier = AppConfig.createNotifier();

    DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(sources, analyzer, notifier);
    orchestrator.execute();
}
```
* `public static void main(String[] args)`: Tego wymaga maszyna wirtualna Javy, aby uruchomić program.
* W ciele metody widzimy dokładny proces: (1) Utworzenie zależności przez Config, (2) Przekazanie ich do Orchestratora, (3) Odpalenie głównej metody `execute()`.