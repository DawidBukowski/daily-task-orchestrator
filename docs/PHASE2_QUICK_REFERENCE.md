# Phase 2 Quick Reference Guide

## Component Overview

```
┌─────────────────────────────────────────────────────────┐
│            ClaudeTasksSummarizer                        │
│  (Orchestrator - Wires everything together)             │
└──────┬──────────┬────────────────┬─────────────────────┘
       │          │                │
       ▼          ▼                ▼
  ┌────────┐  ┌────────┐     ┌──────────┐
  │Prompt  │  │Claude  │     │Response  │
  │Builder │  │API     │     │Parser    │
  └────────┘  │Client  │     └──────────┘
              └────────┘
                  ▲
                  │
         ┌────────┴────────┐
         │                 │
    DirectAnthropic   AwsBedrockClaude
         Client            Client
```

---

## File Locations

### New Files
```
src/main/java/com/dailytask/adapters/analyzers/
├── TaskSummarizationPromptBuilder.java
├── ClaudeResponseParser.java
└── ClaudeTaskSummaryResponse.java

src/main/java/com/dailytask/core/ports/
└── ClaudeApiClient.java

src/test/java/com/dailytask/adapters/analyzers/
├── TaskSummarizationPromptBuilderTest.java
├── ClaudeResponseParserTest.java
└── ClaudeTasksSummarizerTest.java
```

### Modified Files
```
src/main/java/com/dailytask/adapters/analyzers/
└── ClaudeTasksSummarizer.java (Updated implementation)

src/main/java/com/dailytask/core/config/
└── AppConfig.java (Added AWS fallback)

pom.xml (AWS Bedrock dependency commented out)
```

---

## Key Classes

### 1. TaskSummarizationPromptBuilder

**Purpose:** Generate structured prompts for Claude

**Usage:**
```java
TaskSummarizationPromptBuilder builder = 
    new TaskSummarizationPromptBuilder();

String systemPrompt = builder.buildSystemPrompt();
String userPrompt = builder.buildUserPrompt(tasks, LocalDate.now());
```

**Output:** Structured text with JSON schema enforcement

---

### 2. ClaudeApiClient (Interface)

**Purpose:** Abstract Claude API communication

**Implementations:**
- `DirectAnthropicClient` (HTTP API)
- `AwsBedrockClaudeClient` (AWS Bedrock) - disabled

**Usage:**
```java
ClaudeApiClient client = new DirectAnthropicClient(config);
String response = client.sendMessage(systemPrompt, userPrompt);
```

**Exception:** `ClaudeApiException` with error types

---

### 3. ClaudeResponseParser

**Purpose:** Parse and validate JSON responses

**Usage:**
```java
ClaudeResponseParser parser = new ClaudeResponseParser();
ClaudeTaskSummaryResponse response = parser.parse(jsonString);

// Access parsed data
String summary = response.summary();
List<String> recommendations = response.recommendations();
List<TaskUpdate> updates = response.taskUpdates();
```

**Error Handling:** Returns fallback on parse errors

---

### 4. ClaudeTasksSummarizer

**Purpose:** Orchestrate task analysis pipeline

**Dependencies:**
- ClaudeApiClient
- TaskSummarizationPromptBuilder
- ClaudeResponseParser

**Usage:**
```java
ClaudeTasksSummarizer summarizer = new ClaudeTasksSummarizer(
    apiClient,
    promptBuilder,
    responseParser
);

TasksSummary result = summarizer.summarize(tasks);
```

**Workflow:**
1. Build prompts
2. Call API
3. Parse response
4. Match task IDs
5. Apply updates
6. Return summary

---

## JSON Schema

Claude must respond with this exact schema:

```json
{
  "summary": "Brief overview (2-3 sentences)",
  "schedule": "Recommended strategy",
  "recommendations": ["Advice 1", "Advice 2"],
  "taskUpdates": [
    {
      "taskId": "task-123",
      "priority": "HIGH",
      "estimatedHours": 3.5,
      "notes": "Justification"
    }
  ]
}
```

**Constraints:**
- `taskId` must match existing task IDs exactly
- `priority` must be: CRITICAL, HIGH, MEDIUM, or LOW
- `estimatedHours` must be positive (can be decimal)
- Empty arrays allowed for `recommendations` and `taskUpdates`

---

## Configuration

### Environment Variables

Required for Claude API:
```bash
export CLAUDE_PROVIDER=ANTHROPIC
export CLAUDE_API_KEY=sk-ant-...
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
```

Optional AWS configuration:
```bash
export CLAUDE_PROVIDER=AWS_BEDROCK
export AWS_REGION=us-east-1
export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
```

### AppConfig

Factory method:
```java
TaskSummarizer analyzer = AppConfig.createAnalyzer();
```

Automatically:
1. Loads configuration from environment
2. Creates appropriate API client
3. Wires dependencies
4. Returns configured summarizer

---

## Testing

### Run Phase 2 Tests Only
```bash
mvn test -Dtest=TaskSummarizationPromptBuilderTest,ClaudeResponseParserTest,ClaudeTasksSummarizerTest
```

### Run Individual Test Class
```bash
mvn test -Dtest=ClaudeResponseParserTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=ClaudeTasksSummarizerTest#summarize_withSuccessfulResponse_shouldApplyUpdates
```

### Expected Output
```
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Error Handling

### No Exceptions to Caller

All errors are caught internally and return safe defaults:

**API Error:**
```java
try {
    String response = apiClient.sendMessage(...);
    // process response
} catch (ClaudeApiException e) {
    logger.error("Claude API call failed: {}", e.getMessage());
    return createFallbackSummary(tasks);
}
```

**Parse Error:**
```java
try {
    return responseParser.parse(jsonResponse);
} catch (Exception e) {
    logger.error("Failed to parse: {}", e.getMessage());
    return ClaudeTaskSummaryResponse.createFallback();
}
```

**Result:** Application continues functioning even when AI is unavailable

---

## Common Operations

### Add New Task Field to Analysis

1. **Update PromptBuilder:**
```java
// In buildUserPrompt()
if (task.getNewField() != null) {
    prompt.append("New Field: ").append(task.getNewField());
}
```

2. **Update Parser** (if field comes from Claude):
```java
// In parseTaskUpdate()
String newField = extractTextField(updateNode, "newField", "default");
```

3. **Update Summarizer** (if applying to tasks):
```java
// In applyUpdateToTask()
Task updatedTask = new Task(
    // ... existing fields ...
    update.newField() // add new field
);
```

### Add New API Client Implementation

1. **Implement Interface:**
```java
public class MyClaudeClient implements ClaudeApiClient {
    @Override
    public String sendMessage(String systemPrompt, String userPrompt)
            throws ClaudeApiException {
        // Implementation
    }
}
```

2. **Update AppConfig:**
```java
private static ClaudeApiClient createClaudeApiClient(Config config) {
    return switch (config.getProvider()) {
        case MY_PROVIDER -> new MyClaudeClient(config);
        // ... other cases
    };
}
```

### Debug API Responses

Enable debug logging:
```java
// In ClaudeResponseParser or ClaudeTasksSummarizer
logger.debug("Raw response: {}", rawResponse);
```

**Warning:** Never log full responses in production (may contain sensitive data)

---

## Performance Tips

### Batch Processing
Process multiple tasks in one API call:
```java
List<Task> batch = tasks.subList(0, Math.min(50, tasks.size()));
TasksSummary result = summarizer.summarize(batch);
```

### Parallel Processing
Run multiple API calls concurrently (future enhancement):
```java
CompletableFuture<TasksSummary> future = 
    CompletableFuture.supplyAsync(() -> 
        summarizer.summarize(tasks)
    );
```

### Caching
Cache recent analyses (future enhancement):
```java
// Check cache first
String cacheKey = generateCacheKey(tasks);
if (cache.contains(cacheKey)) {
    return cache.get(cacheKey);
}
```

---

## Troubleshooting

### Problem: Tests Pass but Integration Fails

**Check:**
1. Environment variables set correctly
2. API key valid
3. Model ID correct for provider
4. Network connectivity

**Debug:**
```bash
export CLAUDE_API_KEY=your-key
export CLAUDE_PROVIDER=ANTHROPIC
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# Run with verbose logging
mvn test -X
```

### Problem: "Task update has no matching task"

**Cause:** Claude returned task ID that doesn't match input

**Fix:**
- Verify prompt includes correct task IDs
- Check Claude's response format
- Update prompt to emphasize ID matching

### Problem: Parser Returns Fallback

**Cause:** Malformed JSON or missing required fields

**Debug:**
1. Enable debug logging in parser
2. Check raw response structure
3. Verify Claude is following schema

### Problem: Priority Not Applied

**Cause:** Invalid priority string from Claude

**Check:**
- Parser logs for priority normalization
- Verify Priority.fromString() fallback to MEDIUM
- Update system prompt to emphasize uppercase enum

---

## Best Practices

### ✅ Do

- **Validate all inputs** before sending to API
- **Log metadata** (sizes, counts) not content
- **Handle nulls** defensively
- **Return fallbacks** on errors
- **Test edge cases** thoroughly
- **Use immutable DTOs** (records)

### ❌ Don't

- **Don't throw exceptions** to callers
- **Don't log sensitive data** (API keys, full responses)
- **Don't assume API availability**
- **Don't ignore parser warnings**
- **Don't skip null checks**
- **Don't mutate existing tasks**

---

## Monitoring

### Key Metrics to Track

1. **API Success Rate**
   - Target: > 99%
   - Alert: < 95%

2. **Response Time**
   - Target: < 2 seconds
   - Alert: > 5 seconds

3. **Parse Success Rate**
   - Target: > 99%
   - Alert: < 98%

4. **Task Update Match Rate**
   - Target: > 95%
   - Alert: < 90%

### Log Patterns to Watch

**Success:**
```
INFO - Successfully analyzed tasks: X recommendations, Y updates
```

**Warning:**
```
WARN - Task update for ID 'xyz' has no matching task
```

**Error:**
```
ERROR - Claude API call failed: [reason]
```

---

## Next Steps (Phase 3)

1. **Enable AWS Bedrock Client**
   - Fix dependency version
   - Uncomment in pom.xml
   - Test integration

2. **Add Retry Logic**
   - Exponential backoff
   - Max retry attempts
   - Circuit breaker

3. **Implement Caching**
   - In-memory cache for recent analyses
   - TTL-based expiration
   - Cache invalidation on task changes

4. **Add Telemetry**
   - OpenTelemetry integration
   - Latency tracking
   - Error rate monitoring

---

## Contact & Support

**Documentation:** See `PHASE2_CLAUDE_INTEGRATION_SUMMARY.md` for detailed implementation docs

**Test Reports:** Check `target/surefire-reports/` after running tests

**Logs:** Check application logs for runtime behavior
