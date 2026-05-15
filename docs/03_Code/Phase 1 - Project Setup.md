# Phase 1 - Project Setup Notes

## What We Built

1. Maven project structure
2. Core interfaces (DataSource, TaskAnalyzer, TaskNotifier)
3. Skeleton implementations
4. Test framework (JUnit 5 + Mockito)
5. Manual dependency injection (AppConfig)

## Key Classes

- `DailyTaskOrchestrator` - main orchestrator
- `AppConfig` - wires dependencies
- `Task`, `RawTask`, `AnalyzedTasks` - domain models
- `DataSource`, `TaskAnalyzer`, `TaskNotifier` - interfaces

## What I Learned

- [ ] Maven project structure
- [ ] How interfaces work
- [ ] What dependency injection is
- [ ] Hexagonal architecture concept
- [ ] JUnit 5 basics
- [ ] Mockito basics

## Commands to Remember

```bash
# Compile
mvn clean package

# Run tests
mvn test

# Run single test
mvn test -Dtest=ClassName

# Run main class
mvn exec:java -Dexec.mainClass="com.dailytask.Main"
```

## Next: Phase 2 (Gmail)

In Phase 2 Step 1, we'll:
- Set up Gmail API
- Implement OAuth2
- Build GmailDataSource

See: [[Phase 2 - Gmail Integration.md]]