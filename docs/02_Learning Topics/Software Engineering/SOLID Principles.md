# SOLID Principles

## What Is SOLID?

Five principles for writing better code:

### S - Single Responsibility
Each class has ONE reason to change

```java
// BAD - two reasons to change
class EmailSender {
    public void sendEmail(Task task) { ... }
    public void generateReport(Task task) { ... }
}

// GOOD - one reason to change
class EmailSender {
    public void sendEmail(Task task) { ... }
}

class ReportGenerator {
    public void generateReport(Task task) { ... }
}
```

### O - Open/Closed
Open for extension, closed for modification

```java
// Instead of modifying TaskAnalyzer, extend it
public interface TaskAnalyzer {
    AnalyzedTasks analyze(List<Task> tasks);
}

// Add new analyzer without changing existing
public class ClaudeTaskAnalyzer implements TaskAnalyzer { ... }
public class RuleBasedAnalyzer implements TaskAnalyzer { ... }
```

### L - Liskov Substitution
Derived classes must be substitutable for base classes

```java
// You can use any DataSource implementation
public void fetch(DataSource source) {
    source.fetch(); // Works for Gmail, University, etc.
}
```

### I - Interface Segregation
Many client-specific interfaces better than one general-purpose

```java
// BAD - forces implementation of unwanted methods
public interface TaskService {
    void fetch();
    void analyze();
    void notify();
}

// GOOD - segregated interfaces
public interface DataSource { void fetch(); }
public interface TaskAnalyzer { void analyze(); }
public interface TaskNotifier { void notify(); }
```

### D - Dependency Inversion
Depend on abstractions, not concrete implementations

```java
// BAD - depends on concrete GmailDataSource
public class Orchestrator {
    private GmailDataSource source = new GmailDataSource();
}

// GOOD - depends on DataSource interface
public class Orchestrator {
    private DataSource source;
    public Orchestrator(DataSource ds) {
        this.source = ds;
    }
}
```

## In Our Project

We use all 5:
- **S**: Each class has one job
- **O**: Add new DataSource without changing core
- **L**: All DataSources work the same
- **I**: Small focused interfaces
- **D**: Core depends on interfaces, not implementations

Resources:
- Uncle Bob's blog: https://en.wikipedia.org/wiki/SOLID