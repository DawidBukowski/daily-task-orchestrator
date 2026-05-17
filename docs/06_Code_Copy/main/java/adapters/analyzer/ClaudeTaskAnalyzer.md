Udaje, że komunikuje się z API Claude AI w celu przetworzenia zadań.
```java
public class ClaudeTaskAnalyzer implements TaskAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeTaskAnalyzer.class);

    @Override
    public AnalyzedTasks analyze(List<Task> tasks) {
        logger.info("Analyzing {} tasks using Claude AI...", tasks.size());
        
        return new AnalyzedTasks(
                tasks,
                "Dummy Summary: You have " + tasks.size() + " tasks pending.",
                "Dummy Schedule: Do everything ASAP.",
                new ArrayList<>()
        );
    }
}
```
* `implements TaskAnalyzer`: Gwarantuje, że klasa posiada metodę `analyze`.
* `tasks.size()`: Zlicza ile elementów znajduje się na liście. Przekazujemy tę wartość do loggera za pomocą znaczników `{}` (np. "Analyzing 5 tasks...").
* `new AnalyzedTasks(...)`: Ponieważ nie podpięliśmy jeszcze prawdziwego API Claude'a, tworzymy "zaślepkę" (ang. _dummy/stub_). Zwracamy obiekt ze sztucznym tekstem.