# System Design - Daily Task Orchestrator

## High Level Architecture

Multiple Data Sources (Gmail, University, Professors)
↓ 
Data Fetching Layer 
↓ 
RawTask Objects 
↓ 
Data Normalization Layer 
↓ 
Task Objects 
↓ 
Claude API Analysis
↓
AnalyzedTasks 
↓
Email Formatting & Sending
↓ Daily Email to User
## Layers

### 1. Data Sources (Adapters)
- GmailDataSource
- UniversityDataSource
- ProfessorDataSource (future)

### 2. Business Logic (Core)
- TaskNormalizer
- TaskAnalyzer
- DailyTaskOrchestrator

### 3. Output (Adapters)
- EmailTaskNotifier
- SlackNotifier (future)

## Key Interfaces

- `DataSource` - Fetch from any source
- `TaskAnalyzer` - Analyze tasks
- `TaskNotifier` - Send notifications

## Data Models

- `RawTask` - Raw data from source
- `Task` - Normalized domain model
- `AnalyzedTasks` - Final analysis result

See: [[Hexagonal Architecture.md]]