# Dependency Injection (DI)

## What Is It?

Instead of creating dependencies inside a class, you **give them to the class**.

## Bad Way (Without DI)

```java
class TaskOrchestrator {
    private GmailDataSource dataSource = new GmailDataSource();
    private ClaudeTaskAnalyzer analyzer = new ClaudeTaskAnalyzer();
    
    public void execute() {
        dataSource.fetch();
        analyzer.analyze(...);
    }
}
```

**Problems:**
- Hard-coded to Gmail (can't switch to Outlook)
- Can't test without real Gmail
- Changes to Gmail constructor breaks everything
- Tight coupling

## Good Way (With DI)

```java
class TaskOrchestrator {
    private DataSource dataSource;
    private TaskAnalyzer analyzer;
    
    // Dependencies injected (given) to constructor
    public TaskOrchestrator(DataSource ds, TaskAnalyzer ta) {
        this.dataSource = ds;
        this.analyzer = ta;
    }
    
    public void execute() {
        dataSource.fetch();
        analyzer.analyze(...);
    }
}

// Usage
DataSource gmail = new GmailDataSource();
TaskAnalyzer claude = new ClaudeTaskAnalyzer();
TaskOrchestrator app = new TaskOrchestrator(gmail, claude);
app.execute();

// For testing
DataSource mockGmail = new MockDataSource();
TaskAnalyzer mockClaude = new MockTaskAnalyzer();
TaskOrchestrator testApp = new TaskOrchestrator(mockGmail, mockClaude);
testApp.execute();
```

**Benefits:**
- Easy to swap implementations
- Easy to test with mocks
- Dependencies are explicit
- Loose coupling

## Phase 1: Manual DI

We implement DI manually in `AppConfig` class:

```java
class AppConfig {
    public static DataSource createDataSource() {
        return new GmailDataSource(...);
    }
    
    public static TaskAnalyzer createAnalyzer() {
        return new ClaudeTaskAnalyzer(...);
    }
    
    public static TaskNotifier createNotifier() {
        return new EmailTaskNotifier(...);
    }
}
```

Phase 6+: We could use Spring Framework for automatic DI.

See: [[Hexagonal Architecture.md]]