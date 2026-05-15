# Useful Code Snippets

## Maven Commands

### Build
```bash
mvn clean package
```

### Test
```bash
mvn test
mvn test -Dtest=TestClassName
```

### Run
```bash
mvn exec:java -Dexec.mainClass="com.dailytask.Main"
```

## Java Patterns

### Dependency Injection Example
```java
// Constructor injection
public class TaskOrchestrator {
    private DataSource source;
    private TaskAnalyzer analyzer;
    
    public TaskOrchestrator(DataSource s, TaskAnalyzer a) {
        this.source = s;
        this.analyzer = a;
    }
}
```

### Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void myMethod() {
        logger.info("This is a log message");
        logger.error("Something went wrong", exception);
    }
}
```

### Testing with Mocks
```java
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

@Test
public void testFetch() {
    DataSource mockSource = mock(DataSource.class);
    when(mockSource.fetch()).thenReturn(new ArrayList<>());
    
    TaskOrchestrator orchestrator = new TaskOrchestrator(mockSource);
    orchestrator.execute();
    
    verify(mockSource).fetch();
}
```

---

(More snippets will be added as you build each phase)