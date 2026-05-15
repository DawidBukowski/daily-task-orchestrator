# Hexagonal Architecture (Ports & Adapters)

## What Is It?

An architecture pattern that:
- Isolates business logic from external concerns
- Makes code testable without external services
- Allows swapping implementations easily

## Key Principles

1. **Core doesn't know about adapters**
   - Core = business logic
   - Adapters = implementations
   
2. **Interfaces define contracts**
   - Core talks to interfaces
   - Adapters implement interfaces
   
3. **Easy to test**
   - Mock the interfaces
   - Test core in isolation
   
4. **Easy to extend**
   - Add new adapter = add new implementation
   - No changes to core

## Benefits

✅ Testability
✅ Flexibility
✅ Maintainability
✅ Scalability

See: [[Interfaces.md]], [[Dependency Injection.md]]