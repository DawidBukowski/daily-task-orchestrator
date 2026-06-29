```java
public static void main(String[] args) {
    List<DataSource> sources = AppConfig.createDataSources();
    TaskExtractor extractor = AppConfig.createTaskExtractor();
    TaskSummarizer summarizer = AppConfig.createAnalyzer();
    TaskNotifier notifier = AppConfig.createNotifier();

    DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(sources, extractor, summarizer, notifier);
    orchestrator.execute();
}
```
* `public static void main(String[] args)`: Tego wymaga maszyna wirtualna Javy, aby uruchomić program.
* W ciele metody widzimy dokładny proces: (1) Utworzenie zależności przez Config (źródła danych, ekstraktor zadań, analizator, notyfikator), (2) Przekazanie ich do Orchestratora, (3) Odpalenie głównej metody `execute()`.
* Nowy port [[TaskExtractor]] (implementacja: [[SimpleTaskExtractor]]) jest odpowiedzialny za normalizację surowych danych ([[RawData]]) w strukturalne zadania ([[Task]]) używając [[DeadlineParser]], [[TitleExtractor]] i [[TaskFactory]].