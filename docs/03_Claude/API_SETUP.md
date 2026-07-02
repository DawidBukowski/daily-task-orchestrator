# Claude API Client Setup Guide

This guide explains how to configure and use the multi-provider Claude API client in the Daily Task Orchestrator application.

## Overview

The application supports **two providers** for Claude AI integration:

1. **Direct Anthropic API** - Connect directly to Anthropic's Claude API
2. **AWS Bedrock** - Use Claude models through AWS Bedrock Runtime

The provider selection is controlled via environment variables, allowing flexible deployment configurations.

---

## Configuration

### Required Environment Variables

#### Common Variables (Both Providers)

```bash
# Provider selection: ANTHROPIC or AWS_BEDROCK
CLAUDE_PROVIDER=ANTHROPIC

# The Claude model identifier
CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
```

#### Optional Variables (Both Providers)

```bash
# Maximum tokens in Claude's response (default: 1000)
CLAUDE_MAX_TOKENS=1000

# Temperature for response randomness, 0.0-1.0 (default: 0.3)
CLAUDE_TEMPERATURE=0.3

# Request timeout in seconds (default: 30)
CLAUDE_TIMEOUT_SECONDS=30
```

---

## Provider-Specific Setup

### Option 1: Direct Anthropic API

**Required Environment Variables:**

```bash
CLAUDE_PROVIDER=ANTHROPIC
CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
ANTHROPIC_API_KEY=sk-ant-api03-...
```

**Optional:**

```bash
# Override default API endpoint (default: https://api.anthropic.com/v1/messages)
ANTHROPIC_API_URL=https://api.anthropic.com/v1/messages
```

**Getting Your API Key:**

1. Sign up at [console.anthropic.com](https://console.anthropic.com/)
2. Navigate to **API Keys** section
3. Create a new API key
4. Copy the key and set it as `ANTHROPIC_API_KEY`

**Recommended Model IDs:**
- `claude-3-5-sonnet-20241022` - Best balance of intelligence and cost
- `claude-3-opus-20240229` - Maximum intelligence, higher cost
- `claude-3-haiku-20240307` - Fastest, most cost-effective

---

### Option 2: AWS Bedrock

**Required Environment Variables:**

```bash
CLAUDE_PROVIDER=AWS_BEDROCK
CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
AWS_REGION=us-east-1
```

**AWS Credentials:**

The application uses AWS SDK's `DefaultCredentialsProvider`, which supports multiple authentication methods:

1. **Environment variables:**
   ```bash
   AWS_ACCESS_KEY_ID=your_access_key
   AWS_SECRET_ACCESS_KEY=your_secret_key
   ```

2. **AWS credentials file** (`~/.aws/credentials`):
   ```ini
   [default]
   aws_access_key_id = your_access_key
   aws_secret_access_key = your_secret_key
   ```

3. **IAM roles** (when running on EC2, ECS, Lambda, etc.)

4. **AWS SSO**

**Required IAM Permissions:**

Your AWS credentials need the following permissions:

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

**Model Access:**

Before using AWS Bedrock, you must request model access:

1. Go to AWS Console → Amazon Bedrock
2. Navigate to **Model access**
3. Request access to **Anthropic Claude** models
4. Wait for approval (usually instant for most regions)

**Recommended Model IDs (AWS Bedrock):**
- `anthropic.claude-3-5-sonnet-20241022-v2:0` - Latest Sonnet
- `anthropic.claude-3-opus-20240229-v1:0` - Opus
- `anthropic.claude-3-haiku-20240307-v1:0` - Haiku

**Supported Regions:**
- `us-east-1` (US East, N. Virginia) - recommended
- `us-west-2` (US West, Oregon)
- `eu-west-1` (Europe, Ireland)
- `ap-southeast-1` (Asia Pacific, Singapore)
- Check [AWS Bedrock documentation](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html) for current region availability

---

## Example Configurations

### Development - Direct Anthropic API

Create a `.env` file or set environment variables:

```bash
# Provider
CLAUDE_PROVIDER=ANTHROPIC
CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# Credentials
ANTHROPIC_API_KEY=sk-ant-api03-...

# Optional tuning
CLAUDE_MAX_TOKENS=2000
CLAUDE_TEMPERATURE=0.5
CLAUDE_TIMEOUT_SECONDS=60
```

### Production - AWS Bedrock with IAM Role

```bash
# Provider (IAM role provides credentials automatically)
CLAUDE_PROVIDER=AWS_BEDROCK
CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
AWS_REGION=us-east-1

# Optional tuning
CLAUDE_MAX_TOKENS=1500
CLAUDE_TEMPERATURE=0.3
CLAUDE_TIMEOUT_SECONDS=45
```

---

## Architecture

### Components

1. **ClaudeConfiguration** (`com.dailytask.core.config`)
   - Loads and validates environment variables
   - Provides configuration to API clients

2. **ClaudeApiClient** (interface in `com.dailytask.core.ports`)
   - Port interface defining the contract for Claude API communication
   - Implementations handle provider-specific protocols

3. **DirectAnthropicClient** (`com.dailytask.adapters.analyzers`)
   - Connects directly to Anthropic's Messages API
   - Uses Java 21 HttpClient (no external HTTP library needed)

4. **AwsBedrockClaudeClient** (`com.dailytask.adapters.analyzers`)
   - Uses AWS SDK v2 Bedrock Runtime client
   - Implements Bedrock Converse API

5. **AppConfig** (`com.dailytask.core.config`)
   - Factory that wires the appropriate client based on provider selection
   - Integrates with the rest of the application

### Error Handling

Both clients provide detailed error types via `ClaudeApiClient.ClaudeApiException`:

- `AUTHENTICATION_FAILED` - Invalid API key or AWS credentials
- `RATE_LIMIT_EXCEEDED` - Too many requests, retry with backoff
- `SERVER_ERROR` - Provider-side error (5xx)
- `TIMEOUT` - Request exceeded configured timeout
- `MALFORMED_RESPONSE` - Unexpected response format
- `NETWORK_ERROR` - Connection issues
- `INVALID_REQUEST` - Client error (4xx)

---

## Testing

### Verify Configuration

Run the application with logging enabled to see which provider is being used:

```bash
mvn clean install
mvn exec:java
```

Look for log messages like:

```
INFO  c.d.c.c.AppConfig - Initializing Claude API client with provider: ANTHROPIC, model: claude-3-5-sonnet-20241022
INFO  c.d.c.c.AppConfig - Using direct Anthropic API client
```

### Run Unit Tests

```bash
mvn test
```

The `ClaudeConfigurationTest` suite validates configuration loading and validation logic.

---

## Troubleshooting

### Direct Anthropic API

**Error: Authentication failed**
- Verify your `ANTHROPIC_API_KEY` is correct
- Check that the key hasn't been revoked in the Anthropic console
- Ensure no extra whitespace in the environment variable

**Error: Rate limit exceeded**
- Anthropic has rate limits per API key tier
- Implement exponential backoff (future enhancement)
- Consider upgrading your API plan

### AWS Bedrock

**Error: Authentication failed**
- Verify AWS credentials are properly configured
- Check IAM permissions include `bedrock:InvokeModel`
- Ensure the region supports Bedrock

**Error: Model not found or access denied**
- Request model access in the AWS Bedrock console
- Verify the model ID format is correct for Bedrock (e.g., `anthropic.claude-3-5-sonnet-20241022-v2:0`)
- Check that the model is available in your selected region

**Error: Timeout**
- AWS Bedrock has default timeouts
- Increase `CLAUDE_TIMEOUT_SECONDS` if needed
- Check network connectivity to AWS

---

## Cost Considerations

### Direct Anthropic API

Pricing per million tokens (as of 2024):
- **Claude 3.5 Sonnet**: $3 input / $15 output
- **Claude 3 Opus**: $15 input / $75 output
- **Claude 3 Haiku**: $0.25 input / $1.25 output

### AWS Bedrock

Pricing per million tokens (as of 2024, us-east-1):
- **Claude 3.5 Sonnet**: $3 input / $15 output
- **Claude 3 Opus**: $15 input / $75 output
- **Claude 3 Haiku**: $0.25 input / $1.25 output

**Note:** AWS Bedrock may have additional data transfer costs.

---

## Security Best Practices

1. **Never commit API keys** to version control
2. **Use environment variables** or secrets management services (AWS Secrets Manager, HashiCorp Vault)
3. **Rotate API keys** regularly
4. **Use IAM roles** in production (AWS Bedrock)
5. **Implement rate limiting** to prevent runaway API costs
6. **Monitor API usage** via provider dashboards

---

## Future Enhancements

Potential improvements to consider:

- Retry logic with exponential backoff
- Response caching to reduce API calls
- Token usage tracking and cost monitoring
- Support for streaming responses
- Batch processing for multiple tasks
- Fine-tuned prompt optimization per provider

---

## References

- [Anthropic API Documentation](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
