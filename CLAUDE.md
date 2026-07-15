# Claude Integration Documentation

## Project Overview

The Daily Task Orchestrator is a Java-based application for orchestrating and managing daily tasks efficiently with AI-powered summarization and analysis.

### Technology Stack

- **Runtime:** Java 21
- **Framework:** Spring Boot 3.2.5
- **Build Tool:** Maven 3.6+
- **AI Integration:** Claude (Anthropic API and AWS Bedrock)
- **External APIs:** Gmail API (OAuth2)
- **Logging:** SLF4J with Logback
- **Testing:** JUnit 5, Mockito 5.12

### Project Structure

```
daily-task-orchestrator/
├── src/main/java/com/dailytask/
│   ├── core/
│   │   ├── domain/           # Domain models (Task, Priority, TaskStatus, etc.)
│   │   ├── ports/            # Port interfaces (ClaudeApiClient, TaskSummarizer, etc.)
│   │   ├── usecases/         # Use cases (DailyTaskOrchestrator)
│   │   └── config/           # Configuration classes
│   ├── adapters/
│   │   ├── analyzers/        # Claude clients, task extractors, parsers
│   │   ├── datasources/      # Gmail API adapter (OAuth2, SecretsManagerDataStore)
│   │   ├── notifiers/        # Email notification adapter
│   │   └── secrets/          # AWS Secrets Manager provider
│   ├── lambda/               # AWS Lambda handler (DailyTaskLambdaHandler)
│   └── Main.java             # Local entry point
├── scripts/aws-deployment/   # AWS deployment automation (8 scripts)
├── docs/06_AWS_Deployment/   # AWS deployment documentation
├── template.yaml             # SAM CLI template
├── pom.xml                   # Maven build configuration
├── README.md                 # Quick start guide
└── CLAUDE.md                 # This file
```

---

## Claude Integration Architecture

### Overview

The application supports **dual-provider Claude integration**, allowing you to use Claude AI through either:
1. **Direct Anthropic API** - Connect directly to Anthropic's Claude API
2. **AWS Bedrock** - Use Claude models through AWS Bedrock Runtime

This flexibility enables seamless provider switching without code changes, using only environment variable configuration.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Main Application                      │
│              (DailyTaskOrchestrator)                     │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
         ┌─────────────────────────┐
         │    ClaudeConfiguration  │
         │  (Loads Env Variables)  │
         └──────────┬──────────────┘
                    │
         ┌──────────▼──────────┐
         │  AppConfig Factory  │
         │ (Selects Provider)  │
         └──────────┬──────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
   ┌────────┐  ┌──────────┐  ┌─────────────┐
   │ Logger │  │Port Intf │  │ ClaudeTask  │
   │        │  │(Abstract)│  │Summarizer   │
   └────────┘  └────┬─────┘  └─────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
  ┌──────────────┐ ┌────────────────────┐
  │  Anthropic   │ │  AWS Bedrock       │
  │  HTTP Client │ │  Runtime Client    │
  └──────────────┘ └────────────────────┘
        │           │
        └───────────┴───────────┐
                                ▼
                      Claude AI Models
```

### Key Components

#### 1. ClaudeConfiguration (`com.dailytask.core.config`)
- **Purpose:** Load and validate Claude configuration from environment variables
- **Responsibilities:**
  - Parse provider selection (ANTHROPIC or AWS_BEDROCK)
  - Validate required fields based on provider
  - Provide configuration to clients
- **Key Methods:**
  - `fromEnv()`: Static factory loading environment variables
  - `validate()`: Ensures all required config is present

#### 2. ClaudeApiClient (`com.dailytask.core.ports`)
- **Purpose:** Port interface abstracting Claude API communication
- **Responsibility:** Define the contract for sending messages to Claude
- **Key Method:**
  - `sendMessage(String systemPrompt, String userPrompt): String`
- **Error Handling:** Throws `ClaudeApiException` with detailed error types

#### 3. DirectAnthropicClient (`com.dailytask.adapters.analyzers`)
- **Purpose:** Implementation connecting directly to Anthropic's Messages API
- **Transport:** Java 21 built-in `HttpClient` (no external HTTP library)
- **Features:**
  - JSON message serialization/deserialization
  - Direct authentication via `x-api-key` header
  - Configurable timeouts and model selection

#### 4. AwsBedrockClaudeClient (`com.dailytask.adapters.analyzers`)
- **Purpose:** Implementation using AWS Bedrock Runtime for Claude access
- **Transport:** AWS SDK v2 Bedrock Runtime client
- **Features:**
  - Automatic credential resolution (IAM, environment, credentials file)
  - Converse API for message handling
  - Region-specific endpoint configuration

#### 5. ClaudeTasksSummarizer (`com.dailytask.adapters.analyzers`)
- **Purpose:** Main use case for task summarization using Claude
- **Dependencies:**
  - `ClaudeApiClient`: Send prompts and receive responses
  - `TaskSummarizationPromptBuilder`: Format prompts
  - `ClaudeResponseParser`: Parse Claude's JSON responses
- **Key Method:**
  - `summarize(List<Task> tasks): TasksSummary`

#### 6. AppConfig (`com.dailytask.core.config`)
- **Purpose:** Factory for wiring all components together
- **Key Factory Methods:**
  - `createAnalyzer()`: Creates ClaudeTasksSummarizer with appropriate client
  - `createClaudeApiClient(ClaudeConfiguration)`: Selects and instantiates correct client

---

## Environment Setup

### Required Configuration

All Claude integration configuration is **environment-based**. No hardcoded values or config files needed.

#### Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `CLAUDE_PROVIDER` | Yes | N/A | `ANTHROPIC` or `AWS_BEDROCK` |
| `CLAUDE_MODEL_ID` | Yes | N/A | Model identifier for your provider |
| `CLAUDE_MAX_TOKENS` | No | 1000 | Maximum tokens in Claude's response |
| `CLAUDE_TEMPERATURE` | No | 0.3 | Response randomness (0.0-1.0) |
| `CLAUDE_TIMEOUT_SECONDS` | No | 30 | Request timeout in seconds |

### Provider-Specific Variables

#### Anthropic Direct API

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `ANTHROPIC_API_KEY` | Yes | N/A | Your Anthropic API key |
| `ANTHROPIC_API_URL` | No | `https://api.anthropic.com/v1/messages` | API endpoint (rarely overridden) |

**Getting Your API Key:**
1. Visit [console.anthropic.com](https://console.anthropic.com/)
2. Navigate to **API Keys** section
3. Create a new key
4. Copy and store securely

**Recommended Models:**
- `claude-3-5-sonnet-20241022` - Best balance of cost and capability
- `claude-3-opus-20240229` - Maximum intelligence
- `claude-3-haiku-20240307` - Fastest, most economical

#### AWS Bedrock

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `AWS_REGION` | Yes | N/A | AWS region (e.g., `us-east-1`) |
| `AWS_ACCESS_KEY_ID` | Conditional | N/A | AWS access key (or use credentials file/IAM) |
| `AWS_SECRET_ACCESS_KEY` | Conditional | N/A | AWS secret key (or use credentials file/IAM) |

**AWS Credential Resolution Order:**
1. Environment variables: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
2. Credentials file: `~/.aws/credentials`
3. IAM role (EC2, ECS, Lambda)
4. AWS SSO

**Required IAM Permissions:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:InvokeModelWithResponseStream"
      ],
      "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude*"
    }
  ]
}
```

**Before First Use:**
1. Go to AWS Console → Amazon Bedrock
2. Navigate to **Model access**
3. Request access to **Anthropic Claude** models
4. Wait for approval (usually instant)

**Supported Regions:**
- `us-east-1` (US East, N. Virginia) - recommended
- `us-west-2` (US West, Oregon)
- `eu-west-1` (Europe, Ireland)
- `ap-southeast-1` (Asia Pacific, Singapore)

**Recommended Models (Bedrock Format):**
- `anthropic.claude-3-5-sonnet-20241022-v2:0` - Latest Sonnet
- `anthropic.claude-3-opus-20240229-v1:0` - Opus
- `anthropic.claude-3-haiku-20240307-v1:0` - Haiku

### Quick-Start Examples

#### Development with Anthropic Direct

Create a `.env` file or export variables:

```bash
# Provider
export CLAUDE_PROVIDER=ANTHROPIC
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# Credentials
export ANTHROPIC_API_KEY=sk-ant-api03-...

# Optional tuning
export CLAUDE_MAX_TOKENS=2000
export CLAUDE_TEMPERATURE=0.5
export CLAUDE_TIMEOUT_SECONDS=60
```

Then build and run:
```bash
mvn clean package
java -jar target/daily-task-orchestrator.jar
```

#### Production with AWS Bedrock and IAM Role

When running on EC2/ECS/Lambda with an IAM role attached:

```bash
# Provider
export CLAUDE_PROVIDER=AWS_BEDROCK
export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
export AWS_REGION=us-east-1

# AWS credentials come from IAM role (no env vars needed)

# Optional tuning
export CLAUDE_MAX_TOKENS=1500
export CLAUDE_TEMPERATURE=0.3
export CLAUDE_TIMEOUT_SECONDS=45
```

---

## Build & Test Commands

### Build the Project

```bash
# Clean build
mvn clean compile

# Full build with tests
mvn clean package
```

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClaudeConfigurationTest

# Run with detailed output
mvn test -X
```

### Run the Application

```bash
# With environment variables set
java -jar target/daily-task-orchestrator.jar

# Or set variables inline (Linux/Mac)
CLAUDE_PROVIDER=ANTHROPIC \
CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022 \
ANTHROPIC_API_KEY=your-key \
java -jar target/daily-task-orchestrator.jar
```

### View Logs

The application uses SLF4J with Logback. Logs are printed to console and include:
- Configuration loading details
- Provider selection
- Request/response summaries (no sensitive data logged)
- Errors with helpful context

---

## Development Guidelines

### Adding a New Claude Provider

To integrate a new Claude provider (e.g., Google Vertex AI):

1. **Create Configuration Class** (if needed)
   ```java
   // com.dailytask.core.config.YourProviderConfiguration
   public class YourProviderConfiguration {
       // Load from env vars
       // Validate configuration
   }
   ```

2. **Implement ClaudeApiClient Interface**
   ```java
   // com.dailytask.adapters.analyzers.YourProviderClaudeClient
   public class YourProviderClaudeClient implements ClaudeApiClient {
       @Override
       public String sendMessage(String systemPrompt, String userPrompt) 
           throws ClaudeApiException {
           // Implement provider-specific logic
       }
   }
   ```

3. **Add Provider to ClaudeConfiguration Enum**
   ```java
   public enum Provider {
       ANTHROPIC,
       AWS_BEDROCK,
       YOUR_PROVIDER  // Add here
   }
   ```

4. **Update AppConfig.createClaudeApiClient()**
   ```java
   private static ClaudeApiClient createClaudeApiClient(ClaudeConfiguration config) {
       return switch (config.getProvider()) {
           case ANTHROPIC -> new DirectAnthropicClient(config);
           case AWS_BEDROCK -> new AwsBedrockClaudeClient(config);
           case YOUR_PROVIDER -> new YourProviderClaudeClient(config);  // Add here
       };
   }
   ```

5. **Add Tests**
   ```java
   // src/test/java/com/dailytask/adapters/analyzers/YourProviderClaudeClientTest.java
   class YourProviderClaudeClientTest {
       // Test message sending
       // Test error handling
       // Test authentication
   }
   ```

6. **Update Documentation**
   - Add environment variables to this CLAUDE.md
   - Update README.md with setup instructions

### Modifying the JSON Schema

Claude responses are parsed as structured JSON. The current schema expects:

```json
{
  "schedule": "string",
  "tasks": [
    {
      "title": "string",
      "priority": "CRITICAL|HIGH|MEDIUM|LOW",
      "deadline": "YYYY-MM-DDTHH:mm:ss",
      "status": "PENDING|IN_PROGRESS|COMPLETED|CANCELLED"
    }
  ]
}
```

To modify this schema:

1. **Update the Prompt** (`TaskSummarizationPromptBuilder`)
   ```java
   public String buildSystemPrompt() {
       return "Return JSON with new field: ...";
   }
   ```

2. **Update the Parser** (`ClaudeResponseParser`)
   ```java
   public TasksSummary parseResponse(String response) {
       // Parse new JSON structure
   }
   ```

3. **Update Domain Models** if needed
   ```java
   public class TasksSummary {
       private final String schedule;
       private final List<Task> tasks;
       private final String newField;  // Add here
   }
   ```

4. **Write Tests**
   ```java
   class ClaudeResponseParserTest {
       @Test
       void testParseNewField() {
           // Verify new field is correctly parsed
       }
   }
   ```

### Error Handling Patterns

The application gracefully handles Claude API failures:

**Catch and Log Specific Errors:**
```java
try {
    TasksSummary summary = taskSummarizer.summarize(tasks);
} catch (ClaudeApiClient.ClaudeApiException e) {
    switch (e.getErrorType()) {
        case AUTHENTICATION_FAILED:
            logger.error("Check API key configuration");
            break;
        case RATE_LIMIT_EXCEEDED:
            logger.warn("Rate limited, implement exponential backoff");
            break;
        case TIMEOUT:
            logger.warn("Request timed out, consider increasing CLAUDE_TIMEOUT_SECONDS");
            break;
        default:
            logger.error("Unexpected error: {}", e.getMessage());
    }
}
```

**Never Log Sensitive Data:**
```java
// BAD - exposes API key
logger.info("Using API key: {}", apiKey);

// GOOD - hides sensitive parts
logger.info("Using API key: {}...{}", apiKey.substring(0, 5), apiKey.substring(apiKey.length() - 4));
```

### Testing Checklist

Before submitting changes:

1. **Unit Tests Pass**
   ```bash
   mvn test
   ```

2. **Integration Tests Pass**
   - Test with real Claude API if possible
   - Test with invalid credentials (should fail gracefully)

3. **Code Follows Standards**
   - No hardcoded secrets
   - Meaningful error messages
   - Comprehensive logging (without sensitive data)

4. **Documentation Updated**
   - New environment variables documented
   - New features explained in README or guides

---

## Agent Dispatch Protocol

### When to Use Agent Organizer

Delegate to `agent-organizer` for:
- Adding a new Claude provider (significant complexity)
- Implementing new use cases that integrate with Claude
- Major refactoring of the Claude integration architecture
- Cross-component analysis and design decisions

Example:
```
Task: "Add support for Google Vertex AI as a Claude provider"
→ Use agent-organizer (complex, new provider integration)
```

### When to Handle Directly

Handle directly (without agents) for:
- Updating documentation
- Fixing typos or formatting
- Simple configuration changes
- Environment variable additions

Example:
```
Task: "Update Claude configuration timeout default from 30 to 60 seconds"
→ Handle directly (single-step change)
```

### Follow-Up Escalation

**Simple Task Chain** (same scope) → Handle directly
```
1. Fix typo in CLAUDE.md
2. Update README.md similar typo
3. Run spell check
```

**Moderate Task** (related domain, multiple components) → Use subset of agents
```
1. code-review-pro for architecture
2. backend-architect for design
3. Implement based on feedback
```

**Complex Task** (new domain or major scope) → Re-run agent-organizer
```
Completely new Claude provider integration
with database persistence and streaming
```

---

## Troubleshooting

### General Issues

**"CLAUDE_PROVIDER environment variable is required"**
- Ensure `CLAUDE_PROVIDER` is set to `ANTHROPIC` or `AWS_BEDROCK`
- Check for extra whitespace in the variable
- Verify the variable is exported/set in your shell

**"Initialization failed with an error"**
- Check all required environment variables are set
- Review logs for specific error type
- Verify provider-specific requirements (API key, AWS region)

### Anthropic Direct API

**"Authentication failed" or "401 Unauthorized"**
- Verify `ANTHROPIC_API_KEY` is correct
- Check key hasn't been revoked in Anthropic console
- Ensure no extra whitespace in the key
- Verify key permissions in Anthropic dashboard

**"Rate limit exceeded"**
- Implement exponential backoff (future enhancement)
- Check your API plan tier
- Reduce frequency of API calls

**"Timeout occurred"**
- Increase `CLAUDE_TIMEOUT_SECONDS`
- Check network connectivity
- Consider using AWS Bedrock if available

### AWS Bedrock

**"Authentication failed" or "Access Denied"**
- Verify AWS credentials are correctly configured
- Check IAM role has `bedrock:InvokeModel` permission
- Ensure region supports Bedrock
- Verify model access is requested and approved

**"Model not found" or "The provided model identifier is invalid"**
- Check model ID format: `anthropic.claude-3-5-sonnet-20241022-v2:0`
- Verify model is available in your region
- Request model access in AWS Bedrock console
- Compare your model ID with official AWS documentation

**"Resource not found in region"**
- Verify `AWS_REGION` is set to a region with Bedrock
- Check supported regions (us-east-1, us-west-2, eu-west-1, etc.)
- Ensure model is available in that specific region

**"Service quota exceeded"**
- Implement rate limiting
- Batch requests appropriately
- Contact AWS Support for quota increase

---

## Monitoring & Cost Management

### API Usage Monitoring

**Anthropic Direct:**
- Monitor usage via [console.anthropic.com/usage](https://console.anthropic.com/usage)
- Set spending alerts in your account settings
- Track token usage per model

**AWS Bedrock:**
- Monitor via AWS CloudWatch
- Check cost in AWS Cost Explorer
- Use AWS Budgets for spending alerts

### Cost Optimization

**Token Efficiency:**
- Reduce `CLAUDE_MAX_TOKENS` if smaller responses are acceptable
- Use `claude-3-5-sonnet` instead of `claude-3-opus` when possible
- Batch multiple summarization requests

**Provider Selection:**
- Direct Anthropic API: Better for small-scale applications
- AWS Bedrock: Better for enterprise deployments and compliance

### Current Pricing (as of 2024)

**Anthropic Direct API (per million tokens):**
- Claude 3.5 Sonnet: $3 input / $15 output
- Claude 3 Opus: $15 input / $75 output
- Claude 3 Haiku: $0.25 input / $1.25 output

**AWS Bedrock (per million tokens):**
- Claude 3.5 Sonnet: $3 input / $15 output
- Claude 3 Opus: $15 input / $75 output
- Claude 3 Haiku: $0.25 input / $1.25 output

---

## Security Best Practices

### Secrets Management

1. **Never Commit Secrets**
   - Add `.env` to `.gitignore`
   - Don't commit API keys to version control
   - Use environment variables or secrets manager

2. **Use Secrets Management Services**
   - AWS Secrets Manager
   - HashiCorp Vault
   - GitHub Secrets (for CI/CD)
   - 1Password or similar

3. **Rotate Keys Regularly**
   - Anthropic API keys: rotate monthly
   - AWS credentials: rotate per company policy
   - Revoke old keys immediately

### Production Deployment

1. **Use IAM Roles** (AWS Bedrock)
   - Deploy with EC2/ECS IAM roles
   - Avoid hardcoded AWS credentials
   - Audit role permissions regularly

2. **Enable Logging**
   - Log requests (without sensitive data)
   - Monitor for unusual patterns
   - Set up alerts for authentication failures

3. **Implement Rate Limiting**
   - Prevent runaway API costs
   - Implement exponential backoff
   - Set maximum requests per time period

4. **Network Security**
   - Use HTTPS for all API calls (default)
   - Deploy behind VPN/firewall if needed
   - Restrict API key access by IP (if available)

---

## Related Documentation

- **[README.md](README.md)** - Quick start guide and project overview
- **[docs/03_Claude/API_SETUP.md](docs/03_Claude/API_SETUP.md)** - Detailed API setup (reference, some may be outdated)
- **[docs/03_Claude/ARCHITECTURE.md](docs/03_Claude/ARCHITECTURE.md)** - Hexagonal architecture details
- **[docs/03_Claude/TROUBLESHOOTING.md](docs/03_Claude/TROUBLESHOOTING.md)** - Common issues and solutions
- **[docs/00_Meta/PHASE3_IMPLEMENTATION_SUMMARY.md](docs/00_Meta/PHASE3_IMPLEMENTATION_SUMMARY.md)** - Task domain model implementation

---

## References

- [Anthropic API Documentation](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [Spring Boot 3.2 Documentation](https://spring.io/projects/spring-boot)

---

## FAQ

**Q: Can I switch providers after deploying?**
A: Yes, simply change the `CLAUDE_PROVIDER` environment variable and restart. No code changes needed.

**Q: What happens if Claude API is unavailable?**
A: The application throws a `ClaudeApiException` which propagates up. Implement retry logic at the use-case level if needed.

**Q: Which provider should I use?**
A: Start with Anthropic Direct for development (simplest setup). Use AWS Bedrock for production if you're already on AWS.

**Q: How do I test with a mock Claude client?**
A: Create a test implementation of `ClaudeApiClient` that returns predetermined responses. See test files for examples.

**Q: Can I stream Claude responses?**
A: Not currently. This is on the roadmap for Phase 5+.

---

## Contributing

When contributing Claude-related changes:

1. Follow the hexagonal architecture (ports → adapters)
2. Add comprehensive tests with >60% coverage
3. Update this documentation
4. Use meaningful commit messages explaining the "why"
5. Never skip hooks with `--no-verify`
6. Keep secrets out of code and tests

---

---

## Project Phases Summary

### ✅ Phase 1-5: Core Functionality (COMPLETED)
- Gmail API integration with OAuth2
- Task parsing and extraction
- Claude AI integration (Anthropic API + AWS Bedrock)
- Email notifications with SMTP
- Local execution workflow

### ✅ Phase 6d: Lambda Handler Implementation (COMPLETED)
- Lambda handler (`DailyTaskLambdaHandler`) with EventBridge support
- Maven Shade plugin for fat JAR build (~30 MB)
- SAM CLI template for local testing
- Comprehensive unit tests with 7 test methods
- Deployment guide documentation

### ✅ Phase 6e: AWS Infrastructure Setup (COMPLETED)
- **Automated Deployment Scripts:** 7 bash scripts for complete AWS deployment
- **IAM Setup:** Role and policy creation with CloudWatch, Secrets Manager, and Bedrock permissions
- **Secrets Manager:** Automated creation of app-config and gmail-tokens secrets
- **Token Initialization:** Automated Gmail OAuth flow with local execution
- **Lambda Deployment:** Automated function creation/update with configuration
- **Testing:** Automated Lambda invocation with CloudWatch log retrieval

**Key Deliverables (Phase 6e):**
- `scripts/aws-deployment/deploy-all.sh` - Complete one-command deployment
- `scripts/aws-deployment/setup-iam.sh` - IAM role and policy setup
- `scripts/aws-deployment/setup-secrets.sh` - Secrets Manager configuration
- `scripts/aws-deployment/init-gmail-tokens.sh` - Gmail OAuth token initialization
- `scripts/aws-deployment/upload-tokens.sh` - Token upload to Secrets Manager
- `scripts/aws-deployment/deploy-lambda.sh` - Lambda function deployment
- `scripts/aws-deployment/test-lambda.sh` - Lambda function testing
- `scripts/aws-deployment/README.md` - Comprehensive script documentation

### ✅ Phase 6f: EventBridge Scheduling (COMPLETED)
- **EventBridge Rule:** `daily-task-orchestrator-9am` with cron `(0 9 * * ? *)`
- **Lambda Permission:** EventBridge allowed to invoke Lambda function
- **CloudWatch Logs:** 30-day retention configured
- **Setup Script:** `scripts/aws-deployment/setup-eventbridge.sh` - idempotent configuration

### 🔄 Phase 6g: Documentation Finalization & End-to-End Validation (IN PROGRESS)
- Token workflow documentation
- Troubleshooting runbook (AWS operations focus)
- End-to-end validation checklist
- Production readiness review

**Status:** Phase 6g in progress

**Next Steps:**
- Phase 6g: Complete documentation and validation (current)
- Phase 7: University Portal integration (future)
