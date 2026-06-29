Ręczny "Wstrzykiwacz Zależności" (Dependency Injection Container). Zamiast tworzyć obiekty byle gdzie, centralizujemy to.
```java
public static List<DataSource> createDataSources() {
    return List.of(new GmailDataSource());
}

public static TaskExtractor createTaskExtractor() {
    return new SimpleTaskExtractor();
}

public static TaskSummarizer createAnalyzer() {
    return new ClaudeTasksSummarizer();
}

public static TaskNotifier createNotifier() {
    return new EmailTaskNotifier();
}
```
* Metody są `static` – można je wywołać bezpośrednio z nazwy klasy (np. `AppConfig.createAnalyzer()`), bez potrzeby tworzenia obiektu (zmiennego instancji) klasy `AppConfig`. Zwracają typy interfejsów, co ułatwia ewentualną zmianę z `GmailDataSource` na np. `OutlookDataSource`.
* `createTaskExtractor()`: Zwraca implementację portu [[TaskExtractor]]. [[SimpleTaskExtractor]] to kompozycja trzech klas odpowiedzialnych za normalizację surowych danych: [[DeadlineParser]] (parsuje 6 formatów terminów), [[TitleExtractor]] (ekstrakcja tytułów regex), [[TaskFactory]] (orkiestracja tworzenia [[Task]]).