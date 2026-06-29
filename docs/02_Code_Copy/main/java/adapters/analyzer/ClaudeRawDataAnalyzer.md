```markdown
Dummy adapter, który ma w przyszłości wyciągać `Task` z surowych danych przy pomocy Claude AI.
```java
public class ClaudeRawDataAnalyzer implements TaskExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeRawDataAnalyzer.class);

    @Override
    public List<Task> extract(List<RawData> rawData) {
        logger.info("Extracting tasks from {} raw items using Claude AI...", rawData.size());
        return new ArrayList<>();
    }
}
```
* `implements TaskExtractor`: Gwarantuje, że klasa posiada metodę `extract`.
* `rawData.size()`: Zlicza ile surowych rekordów przyszło z portów danych.
* `return new ArrayList<>()`: To jest zaślepka (`dummy/stub`) — prawdziwa implementacja pojawi się później, gdy podepniemy Claude API.
