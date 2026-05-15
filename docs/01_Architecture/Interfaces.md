# Interfaces - Contracts for Our System

## Why Interfaces?

Interfaces define **contracts** - what a component must do, but not how it does it.

## Our Three Main Interfaces

### 1. DataSource

```java
public interface DataSource {
    List<RawTask> fetch();
    String getName();
}
```

**Implementations:**
- `GmailDataSource` - fetches from Gmail
- `UniversityDataSource` - fetches from portal
- `TestDataSource` - test stub

**Why?**
Core doesn't care where data comes from. Just knows it gets RawTasks.

### 2. TaskAnalyzer

```java
public interface TaskAnalyzer {
    AnalyzedTasks analyze(List<Task> tasks);
}
```

**Implementations:**
- `ClaudeTaskAnalyzer` - uses Claude API
- `SimpleTaskAnalyzer` - uses rules
- `MockTaskAnalyzer` - for testing

**Why?**
Core doesn't care HOW analysis happens. Just knows it gets AnalyzedTasks.

### 3. TaskNotifier

```java
public interface TaskNotifier {
    void notify(AnalyzedTasks tasks);
}
```

**Implementations:**
- `EmailTaskNotifier` - sends email
- `SlackTaskNotifier` - sends to Slack
- `MockTaskNotifier` - for testing

**Why?**
Core doesn't care how user is notified. Just knows notification happens.

## Benefits of Interfaces

| Before | After |
|--------|-------|
| Core knows Gmail details | Core only knows DataSource |
| Changing Gmail breaks core | Change Gmail, core untouched |
| Hard to test without real Gmail | Test with MockDataSource |
| Can't add Outlook | Add new DataSource implementation |

## Testing Example

```java
// Without interface - impossible to test without real Gmail
public void execute() {
    gmail.authenticate();
    gmail.fetchEmails();
    // ...
}

// With interface - easy to mock
public void execute() {
    List<RawTask> tasks = dataSource.fetch();
    // Works with real Gmail OR mock
}
```