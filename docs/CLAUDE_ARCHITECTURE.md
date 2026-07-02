# Claude Integration Architecture

## Hexagonal Architecture Overview

The Daily Task Orchestrator follows **hexagonal architecture** (ports and adapters pattern), which enables:
- **Testability** - Easy to mock dependencies
- **Flexibility** - Easy to swap implementations
- **Maintainability** - Clear separation of concerns
- **Scalability** - Add new adapters without changing core logic

### Dependency Flow

```
Core Domain (Cannot depend on anything)
           ↑
           ├── Ports (Interfaces)
           ↑
           ├── Adapters (Implementations)
           ↑
External World (Databases, APIs, UI, etc.)
```

**Key Rule:** Outer layers depend on inner layers, never the reverse.

---

## Component Architecture

### Layer 1: Core Domain
**Location:** `com.dailytask.core.domain`

Domain models that represent business concepts, completely isolated from infrastructure.

```
Domain Models:
├── Task
│   ├── id: String
│   ├── title: String
│   ├── description: String
│   ├── deadline: LocalDateTime
│   ├── priority: Priority (enum)
│   ├── status: TaskStatus (enum)
│   ├── source: String
│   ├── originalId: String
│   └── ... other fields
│
├── TasksSummary
│   ├── schedule: String
│   ├── tasks: List<Task>
│   └── computed methods (getTodaysTasks, getOverdueTasks, etc.)
│
├── Priority (enum)
│   ├── CRITICAL (value: 4)
│   ├── HIGH (value: 3)
│   ├── MEDIUM (value: 2)
│   └── LOW (value: 1)
│
└── TaskStatus (enum)
    ├── PENDING
    ├── IN_PROGRESS
    ├── COMPLETED
    └── CANCELLED
```

**Characteristics:**
- No dependencies on external frameworks
- Pure Java, no annotations
- Business logic only
- Easily testable in isolation

---

### Layer 2: Ports (Interfaces)
**Location:** `com.dailytask.core.ports`

Interfaces that define contracts without implementation details.

#### ClaudeApiClient (Port for Claude Communication)

```java
public interface ClaudeApiClient {
    /**
     * Sends a message to Claude AI.
     * 
     * @param systemPrompt  the system-level instructions for Claude
     * @param userPrompt    the user message with data to analyze
     * @return the raw text response from Claude API
     * @throws ClaudeApiException if the API request fails
     */
    String sendMessage(String systemPrompt, String userPrompt) 
        throws ClaudeApiException;
    
    class ClaudeApiException extends Exception {
        enum ErrorType {
            AUTHENTICATION_FAILED,    // 401 - Invalid credentials
            RATE_LIMIT_EXCEEDED,      // 429 - Too many requests
            SERVER_ERROR,             // 500+ - Provider error
            TIMEOUT,                  // Request exceeded timeout
            MALFORMED_RESPONSE,       // Invalid JSON/format
            NETWORK_ERROR,            // Connection issues
            INVALID_REQUEST           // 400 - Bad request
        }
        
        ErrorType getErrorType();
        int getStatusCode();
    }
}
```

**Why This Design:**
- Allows multiple implementations (Anthropic, AWS Bedrock, Google Vertex, etc.)
- Enables testing with mock implementations
- Decouples use cases from specific providers

#### TaskSummarizer (Port for Task Analysis)

```java
public interface TaskSummarizer {
    /**
     * Summarizes a list of tasks using Claude AI.
     * 
     * @param tasks the tasks to summarize
     * @return a summary with schedule and analysis
     */
    TasksSummary summarize(List<Task> tasks) 
        throws ClaudeApiClient.ClaudeApiException;
}
```

#### TaskExtractor (Port for Task Extraction)

```java
public interface TaskExtractor {
    /**
     * Extracts tasks from raw data sources.
     * 
     * @param rawDataList the raw data to extract from
     * @return a list of extracted tasks
     */
    List<Task> extract(List<RawData> rawDataList);
}
```

#### Other Ports

- **DataSource** - Interface for fetching raw data (Gmail, etc.)
- **TaskNotifier** - Interface for sending notifications

---

### Layer 3: Adapters (Implementations)
**Location:** `com.dailytask.adapters`

Concrete implementations of ports, connecting to external systems.

#### Analyzer Adapters (`adapters/analyzers/`)

```
Analyzers:
├── DirectAnthropicClient (implements ClaudeApiClient)
│   └── Connects directly to Anthropic's Messages API
│       ├── Uses Java 21 HttpClient
│       ├── Serializes/deserializes JSON
│       └── Handles HTTP status codes
│
├── AwsBedrockClaudeClient (implements ClaudeApiClient)
│   └── Connects to AWS Bedrock Runtime
│       ├── Uses AWS SDK v2
│       ├── Handles IAM authentication
│       └── Works with Bedrock Converse API
│
├── ClaudeTasksSummarizer (implements TaskSummarizer)
│   ├── Uses ClaudeApiClient (injected)
│   ├── Uses TaskSummarizationPromptBuilder
│   ├── Uses ClaudeResponseParser
│   └── Orchestrates the summarization workflow
│
├── SimpleTaskExtractor (implements TaskExtractor)
│   ├── Uses TaskFactory
│   ├── Processes RawData list
│   └── Returns extracted Task list
│
├── TaskFactory
│   ├── Converts RawData → Task
│   ├── Uses DeadlineParser
│   └── Uses TitleExtractor
│
├── DeadlineParser
│   └── Parses 6 deadline formats from text
│
├── TitleExtractor
│   └── Extracts meaningful titles from email
│
├── TaskSummarizationPromptBuilder
│   └── Constructs system and user prompts for Claude
│
├── ClaudeResponseParser
│   └── Parses Claude's JSON response into TasksSummary
│
└── ClaudeRawDataAnalyzer
    └── Analyzes raw email for task content
```

#### DataSource Adapters (`adapters/datasources/`)

```
DataSources:
├── GmailDataSource (implements DataSource)
│   ├── GmailApiClient
│   ├── GmailOAuth2Handler (authentication)
│   ├── GmailMessageParser
│   ├── EmailFilter
│   └── EmailToRawDataConverter
│
└── (Future: Slack, Teams, Calendar adapters)
```

#### Notifier Adapters (`adapters/notifiers/`)

```
Notifiers:
└── EmailTaskNotifier (implements TaskNotifier)
    └── Sends email notifications via SMTP
```

---

### Layer 4: Use Cases
**Location:** `com.dailytask.core.usecases`

High-level application logic orchestrating ports and domain.

```java
public class DailyTaskOrchestrator {
    private final List<DataSource> dataSources;        // Port
    private final TaskExtractor taskExtractor;         // Port
    private final TaskSummarizer taskSummarizer;       // Port
    private final TaskNotifier taskNotifier;           // Port
    
    public void orchestrate() {
        // 1. Fetch raw data from sources
        List<RawData> rawData = dataSources.stream()
            .flatMap(source -> source.fetchData().stream())
            .collect(toList());
        
        // 2. Extract tasks from raw data
        List<Task> tasks = taskExtractor.extract(rawData);
        
        // 3. Summarize tasks using AI
        TasksSummary summary = taskSummarizer.summarize(tasks);
        
        // 4. Notify user
        taskNotifier.notify(summary);
    }
}
```

**Key Characteristic:**
- Uses only port interfaces
- No direct dependency on adapters
- Easy to test with mock ports
- Clear, business-focused logic

---

### Layer 5: Configuration
**Location:** `com.dailytask.core.config`

Wires adapters to ports, handling dependency injection.

```java
public class AppConfig {
    
    public static List<DataSource> createDataSources() {
        // Create and wire Gmail-related adapters
        return List.of(new GmailDataSource(...));
    }
    
    public static TaskExtractor createTaskExtractor() {
        // Create and wire task extraction adapters
        return new SimpleTaskExtractor();
    }
    
    public static TaskSummarizer createAnalyzer() {
        // 1. Load configuration from environment
        ClaudeConfiguration config = ClaudeConfiguration.fromEnv();
        
        // 2. Create appropriate Claude client based on provider
        ClaudeApiClient apiClient = createClaudeApiClient(config);
        
        // 3. Create helper components
        TaskSummarizationPromptBuilder promptBuilder = new TaskSummarizationPromptBuilder();
        ClaudeResponseParser responseParser = new ClaudeResponseParser();
        
        // 4. Wire into summarizer
        return new ClaudeTasksSummarizer(apiClient, promptBuilder, responseParser);
    }
    
    private static ClaudeApiClient createClaudeApiClient(ClaudeConfiguration config) {
        return switch (config.getProvider()) {
            case ANTHROPIC -> new DirectAnthropicClient(config);
            case AWS_BEDROCK -> new AwsBedrockClaudeClient(config);
        };
    }
}
```

**Benefits:**
- Single place for wiring dependencies
- Easy to test with different adapters
- Easy to swap implementations

---

### Layer 6: Entry Point
**Location:** `src/main/java/com/dailytask/Main.java`

Bootstrap the application.

```java
public class Main {
    public static void main(String[] args) {
        // 1. Get dependencies from factory
        List<DataSource> dataSources = AppConfig.createDataSources();
        TaskExtractor extractor = AppConfig.createTaskExtractor();
        TaskSummarizer summarizer = AppConfig.createAnalyzer();
        TaskNotifier notifier = AppConfig.createNotifier();
        
        // 2. Inject into use case
        DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(
            dataSources, extractor, summarizer, notifier
        );
        
        // 3. Execute
        orchestrator.orchestrate();
    }
}
```

---

## Claude Integration Flow

### Request Flow (Happy Path)

```
User Code / Use Case
        │
        ▼
DailyTaskOrchestrator.orchestrate()
        │
        ├─→ Fetch data from DataSource (Gmail)
        │   └─→ Convert to List<Task>
        │
        ├─→ Call TaskSummarizer.summarize(tasks)
        │   │
        │   ▼
        │   ClaudeTasksSummarizer
        │   │
        │   ├─→ TaskSummarizationPromptBuilder
        │   │   └─→ Create system + user prompts
        │   │
        │   ├─→ ClaudeApiClient.sendMessage()
        │   │   │
        │   │   ├─→ if provider == ANTHROPIC
        │   │   │   └─→ DirectAnthropicClient
        │   │   │       ├─→ Serialize request to JSON
        │   │   │       ├─→ POST to api.anthropic.com/v1/messages
        │   │   │       └─→ Receive response
        │   │   │
        │   │   └─→ if provider == AWS_BEDROCK
        │   │       └─→ AwsBedrockClaudeClient
        │   │           ├─→ Prepare Converse API request
        │   │           ├─→ Call BedrockRuntime.converse()
        │   │           └─→ Receive response
        │   │
        │   ├─→ ClaudeResponseParser
        │   │   └─→ Parse JSON → TasksSummary
        │   │
        │   └─→ Return TasksSummary
        │
        └─→ Notify user via TaskNotifier
```

### Error Handling Flow

```
ClaudeApiClient.sendMessage()
        │
        ├─→ Catches IOException
        │   └─→ Throws ClaudeApiException(NETWORK_ERROR)
        │
        ├─→ Catches timeout
        │   └─→ Throws ClaudeApiException(TIMEOUT)
        │
        ├─→ HTTP 401/403
        │   └─→ Throws ClaudeApiException(AUTHENTICATION_FAILED)
        │
        ├─→ HTTP 429
        │   └─→ Throws ClaudeApiException(RATE_LIMIT_EXCEEDED)
        │
        ├─→ HTTP 500+
        │   └─→ Throws ClaudeApiException(SERVER_ERROR)
        │
        ├─→ Malformed JSON response
        │   └─→ Throws ClaudeApiException(MALFORMED_RESPONSE)
        │
        └─→ HTTP 400
            └─→ Throws ClaudeApiException(INVALID_REQUEST)

            ↓
        
ClaudeTasksSummarizer catches exception
        │
        └─→ Either:
            ├─→ Log error and rethrow
            ├─→ Retry with exponential backoff (future)
            └─→ Return degraded response (future)
```

---

## Provider Selection Logic

```
Environment Setup
        │
        ▼
CLAUDE_PROVIDER env var
        │
        ├─→ Value: "ANTHROPIC"
        │   │
        │   ├─→ Load ANTHROPIC_API_KEY
        │   ├─→ Load CLAUDE_MODEL_ID
        │   ├─→ Load ANTHROPIC_API_URL (default)
        │   │
        │   ▼
        │   AppConfig.createClaudeApiClient()
        │   │
        │   └─→ return new DirectAnthropicClient(config)
        │       │
        │       └─→ Uses HttpClient to connect
        │           to api.anthropic.com
        │
        └─→ Value: "AWS_BEDROCK"
            │
            ├─→ Load AWS_REGION
            ├─→ Load AWS credentials (via DefaultCredentialsProvider)
            ├─→ Load CLAUDE_MODEL_ID (bedrock format)
            │
            ▼
            AppConfig.createClaudeApiClient()
            │
            └─→ return new AwsBedrockClaudeClient(config)
                │
                └─→ Uses BedrockRuntime to connect
                    to bedrock.{region}.amazonaws.com
```

---

## Key Design Decisions

### 1. Why Ports and Adapters?

**Before (Tightly Coupled):**
```java
public class ClaudeTasksSummarizer {
    private HttpClient httpClient;  // Direct dependency on HTTP
    private DirectAnthropicClient anthropic;  // Can't test without API key
    
    public TasksSummary summarize(List<Task> tasks) {
        // Direct Anthropic API call
    }
}
```

**After (Loose Coupling):**
```java
public class ClaudeTasksSummarizer {
    private final ClaudeApiClient client;  // Abstraction, can be any provider
    
    public ClaudeTasksSummarizer(ClaudeApiClient client, ...) {
        this.client = client;  // Injected
    }
    
    public TasksSummary summarize(List<Task> tasks) {
        // Uses abstract interface, doesn't care about implementation
    }
}
```

**Benefits:**
- Test with mock client
- Switch providers by changing one line
- Anthropic → AWS Bedrock → Google Vertex in the future

### 2. Why Separate Clients?

Direct Anthropic and AWS Bedrock have different APIs:

**DirectAnthropicClient:**
- Uses Java HttpClient
- Calls `POST /v1/messages`
- Sends `x-api-key` header

**AwsBedrockClaudeClient:**
- Uses AWS SDK Bedrock Runtime
- Calls `bedrock:InvokeModel`
- Uses IAM for authentication

Separating them:
- Each can be optimized for its specific API
- Each handles its own error codes
- Each can be tested independently

### 3. Why ClaudeConfiguration?

Configuration from environment variables during construction:

```java
// Static factory method
ClaudeConfiguration config = ClaudeConfiguration.fromEnv();

// Validates at construction time
// Throws exceptions early if misconfigured
// No "runtime configuration" surprises
```

**Benefits:**
- Fail fast if misconfigured
- No null checks during execution
- Configuration is immutable

### 4. Why ResponseParser?

Claude returns JSON; we need TasksSummary:

```java
// Raw JSON from Claude
{
  "schedule": "Morning: Priority 1-2. Afternoon: Projects...",
  "tasks": [...]
}

// Parsed object
TasksSummary summary = new TasksSummary(schedule, tasks);
```

**Why separate?**
- Single responsibility (parsing)
- Easy to test parsing logic
- Easy to change JSON schema (update parser only)
- Reusable if other components need JSON parsing

---

## Testing Architecture

### Unit Testing (Single Component)

```java
@Test
void testDirectAnthropicClientSuccess() {
    // Arrange
    ClaudeConfiguration config = new ClaudeConfiguration.Builder()
        .provider(ANTHROPIC)
        .modelId("claude-3-5-sonnet-20241022")
        .anthropicApiKey("test-key")
        .build();
    
    DirectAnthropicClient client = new DirectAnthropicClient(config);
    
    // Mock HttpClient response...
    
    // Act
    String response = client.sendMessage("system", "user");
    
    // Assert
    assertEquals(expected, response);
}
```

### Integration Testing (Multiple Components)

```java
@Test
void testFullSummarizationPipeline() {
    // Arrange
    List<Task> tasks = List.of(
        new Task("Task 1", Priority.HIGH, LocalDateTime.now()),
        new Task("Task 2", Priority.MEDIUM, LocalDateTime.now())
    );
    
    ClaudeConfiguration config = loadFromEnv();
    ClaudeApiClient client = new DirectAnthropicClient(config);
    TaskSummarizer summarizer = new ClaudeTasksSummarizer(client, ...);
    
    // Act
    TasksSummary summary = summarizer.summarize(tasks);
    
    // Assert
    assertNotNull(summary.getSchedule());
    assertEquals(2, summary.getTasks().size());
}
```

### Testing with Mocks

```java
@Test
void testOrchestratorWithMockSummarizer() {
    // Arrange
    ClaudeApiClient mockClient = mock(ClaudeApiClient.class);
    when(mockClient.sendMessage(any(), any()))
        .thenReturn("{\"schedule\": \"test\", \"tasks\": []}");
    
    TaskSummarizer summarizer = new ClaudeTasksSummarizer(mockClient, ...);
    DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(
        dataSources, extractor, summarizer, notifier
    );
    
    // Act
    orchestrator.orchestrate();
    
    // Assert
    verify(mockClient).sendMessage(any(), any());
}
```

---

## Adding New Components

### Adding a New Provider (Google Vertex AI)

1. **Create Config (if needed):**
   ```java
   public class VertexAiConfiguration {
       // Load from env: VERTEX_AI_PROJECT, VERTEX_AI_LOCATION
   }
   ```

2. **Implement Port:**
   ```java
   public class VertexAiClaudeClient implements ClaudeApiClient {
       @Override
       public String sendMessage(String system, String user) 
           throws ClaudeApiException {
           // Vertex AI specific implementation
       }
   }
   ```

3. **Add Provider Enum:**
   ```java
   public enum Provider {
       ANTHROPIC,
       AWS_BEDROCK,
       VERTEX_AI  // Add here
   }
   ```

4. **Update AppConfig:**
   ```java
   case VERTEX_AI -> {
       logger.info("Using Google Vertex AI client");
       yield new VertexAiClaudeClient(config);
   }
   ```

5. **Test:**
   ```java
   class VertexAiClaudeClientTest {
       // Test authentication
       // Test message sending
       // Test error handling
   }
   ```

---

## Performance Considerations

### Caching

Future enhancement: Cache summaries to reduce API calls.

```
Request came in
    │
    ├─→ Check cache (hash of tasks)
    │   ├─→ Hit: Return cached summary
    │   └─→ Miss: Call Claude
    │
    └─→ Store response in cache
        └─→ Return to caller
```

### Rate Limiting

Implement exponential backoff:

```java
public class RateLimitHandler {
    private int attemptCount = 0;
    private static final int MAX_ATTEMPTS = 3;
    
    public String executeWithRetry(Callable<String> request) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                return request.call();
            } catch (ClaudeApiException e) {
                if (e.getErrorType() == RATE_LIMIT_EXCEEDED) {
                    long backoffMs = 1000 * (long) Math.pow(2, i);
                    Thread.sleep(backoffMs);
                } else {
                    throw e;
                }
            }
        }
    }
}
```

---

## References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) by Alistair Cockburn
- [Ports and Adapters Pattern](https://en.wikipedia.org/wiki/Ports_and_adapters_pattern)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Dependency Injection](https://martinfowler.com/articles/injection.html)

