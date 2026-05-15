# Java Interfaces & Abstraction

## What Is an Interface?

A contract that says:
"If you implement me, you MUST have these methods with these signatures"

## Example

```java
public interface DataSource {
    List<RawTask> fetch();
    String getName();
}
```

Any class that implements DataSource MUST have:
- `fetch()` method returning `List<RawTask>`
- `getName()` method returning `String`

## Why Use Interfaces?

1. **Polymorphism** - treat different implementations the same way
2. **Testing** - easily swap real with mock
3. **Contracts** - explicit about what's needed
4. **Flexibility** - change implementation without changing code that uses it

## In Our Project

```java
// Core doesn't care which implementation
public void processTasks(DataSource source) {
    List<RawTask> tasks = source.fetch();
    // Works with GmailDataSource, UniversityDataSource, etc.
}
```

## Key Points

- Interface = what to do
- Implementation = how to do it
- Code to interfaces, not implementations
- Prefer composition over inheritance

Resources:
- Oracle Java Docs: https://docs.oracle.com/javase/tutorial/java/concepts/interface.html