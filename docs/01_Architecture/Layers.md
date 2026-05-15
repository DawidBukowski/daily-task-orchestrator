# Layered Architecture

## Three Main Layers

### Layer 1: Adapters (External)
- Gmail, University Portal, Email, AWS Lambda
- Technology-specific code
- Changes when external services change
- Examples: `GmailDataSource`, `EmailTaskNotifier`

### Layer 2: Core (Business Logic)
- Task model, Normalizer, Orchestrator
- No external dependencies
- Pure business logic
- Testable without mocks
- Examples: `Task`, `TaskNormalizer`, `DailyTaskOrchestrator`

### Layer 3: Ports (Interfaces)
- DataSource, TaskAnalyzer, TaskNotifier
- Define contracts
- Separate adapters from core
- Examples: interfaces in `core/ports/`

## Data Flow Between Layers
External World
↓(Adapter) 
Gmail Data
↓ (Adapter converts to) 
RawTask
↓ (Normalizer converts to) 
Task
↓ (Analyzer)
AnalyzedTasks
↓ (Formatter) 
Email HTML 
↓(Adapter sends via) 
User's Inbox
## Why This Matters

- **Adapter changes** = change 1 file (GmailDataSource)
- **Core changes** = affects everything (need careful testing)
- **Port changes** = affects all adapters (but core is safe)

Keep core small and focused!