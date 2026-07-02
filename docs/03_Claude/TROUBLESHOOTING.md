# Claude Integration Troubleshooting Guide

This guide covers common issues when using Claude integration with the Daily Task Orchestrator and provides step-by-step solutions.

## Quick Diagnosis

### Check Your Environment Variables

First, verify all required variables are set:

```bash
# Check if variables exist
echo "CLAUDE_PROVIDER: ${CLAUDE_PROVIDER:-(not set)}"
echo "CLAUDE_MODEL_ID: ${CLAUDE_MODEL_ID:-(not set)}"

# For Anthropic
echo "ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY:-(not set)}"

# For AWS Bedrock
echo "AWS_REGION: ${AWS_REGION:-(not set)}"
echo "AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID:-(not set)}"
```

### Check Logs

Enable debug logging to see what's happening:

```bash
# Run with debug logging
mvn test -X 2>&1 | grep -i "claude\|anthropic\|bedrock\|error"

# Or run the jar with modified logging config
java -jar target/daily-task-orchestrator.jar 2>&1 | tail -50
```

---

## Initialization Issues

### "CLAUDE_PROVIDER environment variable is required"

**Symptom:**
```
Exception: CLAUDE_PROVIDER environment variable is required. 
Valid values: ANTHROPIC, AWS_BEDROCK
```

**Causes:**
1. Variable not set at all
2. Variable set but not exported
3. Shell didn't load your `.bash_profile` or `.zshrc`
4. Using different shell than where you set the variable

**Solutions:**

```bash
# 1. Export the variable in current shell
export CLAUDE_PROVIDER=ANTHROPIC

# 2. Verify it's exported
env | grep CLAUDE_PROVIDER

# 3. If using .env file, make sure to source it
source .env
echo $CLAUDE_PROVIDER

# 4. For Docker/containers, pass via -e flag
docker run -e CLAUDE_PROVIDER=ANTHROPIC ...

# 5. For systemd services, add to [Service] section
[Service]
Environment="CLAUDE_PROVIDER=ANTHROPIC"
```

### "Invalid CLAUDE_PROVIDER value: INVALID_VALUE"

**Symptom:**
```
Exception: Invalid CLAUDE_PROVIDER value: INVALID_VALUE. 
Valid values: ANTHROPIC, AWS_BEDROCK
```

**Causes:**
1. Typo in provider name (e.g., "Anthropic" instead of "ANTHROPIC")
2. Extra whitespace: `" ANTHROPIC "` instead of `"ANTHROPIC"`
3. Copy-pasted from documentation with special characters

**Solutions:**

```bash
# Correct spellings (case-sensitive)
export CLAUDE_PROVIDER=ANTHROPIC        # Correct
export CLAUDE_PROVIDER=anthropic        # Wrong
export CLAUDE_PROVIDER="ANTHROPIC "     # Wrong (extra space)

# Remove leading/trailing whitespace
export CLAUDE_PROVIDER=$(echo "ANTHROPIC" | xargs)

# Verify exact value
echo -n "$CLAUDE_PROVIDER" | od -c  # See exact characters
```

### "CLAUDE_MODEL_ID environment variable is required"

**Symptom:**
```
Exception: CLAUDE_MODEL_ID environment variable is required
```

**Solutions:**

```bash
# Set the model ID for your provider
# For Anthropic Direct
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# For AWS Bedrock
export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0

# Verify it's set
echo $CLAUDE_MODEL_ID
```

---

## Authentication Issues

### Anthropic Direct API

#### "ANTHROPIC_API_KEY environment variable is required when using ANTHROPIC provider"

**Symptom:**
```
Exception: ANTHROPIC_API_KEY environment variable is required 
when using ANTHROPIC provider
```

**Solutions:**

```bash
# 1. Set the API key
export ANTHROPIC_API_KEY=sk-ant-api03-...

# 2. Verify it's set (don't print the full key in logs!)
echo "API Key set: $(test -z $ANTHROPIC_API_KEY && echo 'NO' || echo 'YES')"

# 3. Check for extra whitespace
echo -n "$ANTHROPIC_API_KEY" | wc -c  # Should be ~50+ characters

# 4. Get the key from Anthropic console
# https://console.anthropic.com/api-keys
```

#### "Authentication failed" or "401 Unauthorized"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=AUTHENTICATION_FAILED, 
statusCode=401, message='Invalid API key'}
```

**Causes:**
1. API key is expired or revoked
2. API key is incorrect or truncated
3. API key doesn't have necessary permissions
4. API key for a different account

**Solutions:**

```bash
# 1. Verify API key format (should start with sk-ant-api03-)
echo $ANTHROPIC_API_KEY | head -c 20
# Output: sk-ant-api03-...

# 2. Generate a new API key
# https://console.anthropic.com/api-keys
# - Click "Create Key"
# - Copy the full key (don't share)
# - Use the new key

# 3. Check key status in Anthropic console
# https://console.anthropic.com/api-keys
# Look for any warnings or disabled status

# 4. Test with curl (if available)
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{"model":"claude-3-5-sonnet-20241022","max_tokens":1024,"messages":[{"role":"user","content":"Hello"}]}'
```

#### "Invalid model name" or "Model not found"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=INVALID_REQUEST, 
statusCode=400, message='model not found'}
```

**Causes:**
1. Model name doesn't exist
2. Typo in model name
3. Model has been deprecated
4. Wrong model format for provider

**Solutions:**

```bash
# 1. Use correct format for Anthropic Direct
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# NOT these (wrong format):
# export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0  # AWS format
# export CLAUDE_MODEL_ID=claude-3-5-sonnet  # Incomplete
# export CLAUDE_MODEL_ID=claude3.5-sonnet   # Wrong syntax

# 2. Check latest model names
# https://docs.anthropic.com/claude/reference/getting-started-with-the-api

# 3. Common valid models
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022      # Latest Sonnet
export CLAUDE_MODEL_ID=claude-3-opus-20240229          # Opus
export CLAUDE_MODEL_ID=claude-3-haiku-20240307         # Haiku
```

#### "Rate limit exceeded" or "429 Too Many Requests"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=RATE_LIMIT_EXCEEDED, 
statusCode=429, message='rate limit exceeded'}
```

**Causes:**
1. Sending too many requests too quickly
2. Using free/trial API tier with high rate limit
3. Multiple instances of app running with same key
4. API key tier limits exceeded

**Solutions:**

```bash
# 1. Reduce request frequency
# Increase CLAUDE_TIMEOUT_SECONDS to allow more time between requests
export CLAUDE_TIMEOUT_SECONDS=60

# 2. Check your API plan limits
# https://console.anthropic.com/account/usage

# 3. If needed, upgrade your Anthropic plan
# https://console.anthropic.com/account/billing/overview

# 4. Implement exponential backoff (future feature)
# For now, handle manually:
# - Wait longer between requests
# - Batch requests if possible
# - Use a single API key across your application
```

---

### AWS Bedrock

#### "AWS_REGION environment variable is required when using AWS_BEDROCK provider"

**Symptom:**
```
Exception: AWS_REGION environment variable is required 
when using AWS_BEDROCK provider
```

**Solutions:**

```bash
# Set the region
export AWS_REGION=us-east-1

# Valid regions (check current availability)
export AWS_REGION=us-east-1           # US East (N. Virginia) - Recommended
export AWS_REGION=us-west-2           # US West (Oregon)
export AWS_REGION=eu-west-1           # Europe (Ireland)
export AWS_REGION=ap-southeast-1      # Asia Pacific (Singapore)

# Verify it's set
echo $AWS_REGION
```

#### "Authentication failed" or "401 Unauthorized"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=AUTHENTICATION_FAILED, 
statusCode=401, message='Unable to determine AWS credentials'}
```

**Causes:**
1. AWS credentials not configured
2. Credentials are expired
3. Wrong IAM user/role
4. Environment variables not set correctly

**Solutions:**

```bash
# 1. Check if credentials are configured
aws sts get-caller-identity  # Should return your account info

# 2. If not working, configure credentials

# Option A: Environment variables
export AWS_ACCESS_KEY_ID=your_access_key_id
export AWS_SECRET_ACCESS_KEY=your_secret_access_key

# Option B: AWS credentials file
# Create or edit ~/.aws/credentials
[default]
aws_access_key_id = your_access_key_id
aws_secret_access_key = your_secret_access_key

# Option C: AWS SSO
aws sso login --profile my-profile
export AWS_PROFILE=my-profile

# 3. Verify credentials work
aws bedrock list-foundation-models --region $AWS_REGION

# 4. Check credentials haven't expired
# If using temporary credentials, they expire after a time
aws sts get-session-token

# 5. Create new credentials if needed
# https://console.aws.amazon.com/iam/home#/security_credentials
```

#### "Access Denied" or "UnauthorizedOperation"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=AUTHENTICATION_FAILED, 
statusCode=403, message='User: arn:aws:iam::123456789:user/myuser 
is not authorized to perform: bedrock:InvokeModel'}
```

**Causes:**
1. IAM user/role lacks Bedrock permissions
2. IAM policy is too restrictive
3. Model access not requested in Bedrock console

**Solutions:**

```bash
# 1. Check who you are authenticated as
aws sts get-caller-identity

# 2. Add Bedrock permissions to your IAM user/role
# https://console.aws.amazon.com/iam/

# Required minimal policy:
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

# 3. Request model access in AWS Bedrock console
# https://console.aws.amazon.com/bedrock/
# - Click "Model access"
# - Find "Anthropic Claude"
# - Click "Request model access"
# - Wait for approval (usually instant)

# 4. Verify model access
aws bedrock get-foundation-model \
  --model-identifier anthropic.claude-3-5-sonnet-20241022-v2:0 \
  --region $AWS_REGION
```

#### "Model not available in region" or "ResourceNotFoundException"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=INVALID_REQUEST, 
statusCode=400, message='Could not find model with identifier 
anthropic.claude-3-5-sonnet-20241022-v2:0'}
```

**Causes:**
1. Model not available in selected region
2. Wrong model ID format
3. Model hasn't been released yet
4. Using unsupported region

**Solutions:**

```bash
# 1. Check model availability by region
aws bedrock list-foundation-models --region us-east-1 | grep claude

# 2. Use correct AWS Bedrock model format
export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0

# NOT these:
# export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022  # Direct API format
# export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022  # Missing version

# 3. Switch to a region that supports the model
export AWS_REGION=us-east-1

# Recommended regions with Claude:
export AWS_REGION=us-east-1           # Most models available
export AWS_REGION=us-west-2           # US backup
export AWS_REGION=eu-west-1           # Europe

# 4. Check official AWS Bedrock documentation for latest models
# https://docs.aws.amazon.com/bedrock/latest/userguide/
```

---

## Network & Connectivity Issues

### "Timeout occurred" or "Request exceeded timeout"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=TIMEOUT, 
message='Request timed out after 30 seconds'}
```

**Causes:**
1. Network connectivity issue
2. API provider experiencing slowness
3. Timeout too short for your network
4. Request payload too large

**Solutions:**

```bash
# 1. Increase timeout
export CLAUDE_TIMEOUT_SECONDS=60

# 2. Test network connectivity
# For Anthropic
curl -I https://api.anthropic.com/v1/messages

# For AWS Bedrock
aws ec2 describe-regions --region us-east-1

# 3. Check provider status
# Anthropic: https://status.anthropic.com/
# AWS: https://status.aws.amazon.com/

# 4. Try from different network
# Sometimes ISP/firewall blocks certain endpoints
# Try from a different network (mobile hotspot, VPN, etc.)

# 5. Reduce max tokens (smaller response)
export CLAUDE_MAX_TOKENS=500

# 6. Check network latency
ping api.anthropic.com
# AWS Bedrock region endpoint
ping bedrock.us-east-1.amazonaws.com
```

### "Connection refused" or "Connection timeout"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=NETWORK_ERROR, 
message='java.net.ConnectException: Connection refused'}
```

**Causes:**
1. Provider API is down
2. Network firewall blocks the connection
3. ISP is blocking the endpoint
4. Typo in API URL

**Solutions:**

```bash
# 1. Check if provider is accessible
curl -v https://api.anthropic.com/

# 2. Check if DNS is working
nslookup api.anthropic.com
dig api.anthropic.com

# 3. Try with different DNS
# Google DNS: 8.8.8.8
# Cloudflare DNS: 1.1.1.1

# 4. Check if firewall allows HTTPS (port 443)
curl -v --trace-ascii /dev/stdout https://api.anthropic.com/

# 5. If behind corporate proxy, configure proxy settings
# (This would require code changes, contact admin)

# 6. Try VPN if available
# Sometimes ISP blocks certain endpoints
```

---

## Response Parsing Issues

### "Malformed response" or "JSON parsing error"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=MALFORMED_RESPONSE, 
message='Unexpected token at line 1 column 2'}
```

**Causes:**
1. Claude response is not valid JSON
2. Response truncated or incomplete
3. Response in unexpected format
4. Unicode or encoding issues

**Solutions:**

```bash
# 1. Check what Claude is actually returning
# Add debug logging to see raw response

# 2. Verify JSON format
# Save response to file and validate
echo 'response' | jq .

# 3. Increase max tokens (in case response was truncated)
export CLAUDE_MAX_TOKENS=2000

# 4. Check model ID (some models respond differently)
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022

# 5. Review Claude's documentation for response format
# https://docs.anthropic.com/claude/reference/getting-started-with-the-api
```

### "Invalid request" or "400 Bad Request"

**Symptom:**
```
ERROR: ClaudeApiException{errorType=INVALID_REQUEST, 
statusCode=400, message='Invalid request body'}
```

**Causes:**
1. System prompt or user prompt is malformed
2. Prompt contains invalid characters
3. Temperature outside valid range (0.0-1.0)
4. Max tokens is invalid (0 or negative)

**Solutions:**

```bash
# 1. Check temperature is valid (0.0 to 1.0)
export CLAUDE_TEMPERATURE=0.5

# NOT:
# export CLAUDE_TEMPERATURE=2.0  # Too high
# export CLAUDE_TEMPERATURE=-1   # Negative

# 2. Check max tokens is positive
export CLAUDE_MAX_TOKENS=1000

# NOT:
# export CLAUDE_MAX_TOKENS=0     # Zero
# export CLAUDE_MAX_TOKENS=-500  # Negative

# 3. Verify no special characters in prompts that break JSON
# This is usually handled by the code, but check logs

# 4. Check model ID matches provider
# Anthropic: claude-3-5-sonnet-20241022
# AWS: anthropic.claude-3-5-sonnet-20241022-v2:0
```

---

## Configuration Issues

### "Invalid integer value for CLAUDE_MAX_TOKENS"

**Symptom:**
```
Exception: Invalid integer value for CLAUDE_MAX_TOKENS: not_a_number
```

**Solutions:**

```bash
# Use only numeric values
export CLAUDE_MAX_TOKENS=1000         # Correct
export CLAUDE_MAX_TOKENS=1000.5       # Wrong (decimal)
export CLAUDE_MAX_TOKENS="1000"       # OK (will be parsed)
export CLAUDE_MAX_TOKENS=1000a        # Wrong (has letter)
```

### "Invalid double value for CLAUDE_TEMPERATURE"

**Symptom:**
```
Exception: Invalid double value for CLAUDE_TEMPERATURE: not_a_double
```

**Solutions:**

```bash
# Use numeric values between 0.0 and 1.0
export CLAUDE_TEMPERATURE=0.3         # Correct
export CLAUDE_TEMPERATURE=0           # OK
export CLAUDE_TEMPERATURE=1           # OK
export CLAUDE_TEMPERATURE=0.3a        # Wrong

# If you get a parsing error, verify the value
echo $CLAUDE_TEMPERATURE | od -c
```

### "Temperature must be between 0.0 and 1.0"

**Symptom:**
```
Exception: Temperature must be between 0.0 and 1.0
```

**Solutions:**

```bash
# Temperature controls response randomness
export CLAUDE_TEMPERATURE=0.0         # Deterministic responses
export CLAUDE_TEMPERATURE=0.3         # Recommended (more focused)
export CLAUDE_TEMPERATURE=0.5         # Moderate creativity
export CLAUDE_TEMPERATURE=1.0         # Maximum randomness

# NOT:
# export CLAUDE_TEMPERATURE=-0.5      # Too low
# export CLAUDE_TEMPERATURE=1.5       # Too high
```

---

## Logging & Debugging

### Enable Debug Logging

```bash
# Set environment variable for more verbose logging
export DEBUG=1

# Or modify logback.xml to change log level to DEBUG
# In pom.xml, ensure logging is configured

# Run with verbose output
mvn test -X

# Run with specific test
mvn test -Dtest=ClaudeConfigurationTest -X
```

### View Full Error Stack Trace

```bash
# Run and capture full output
java -jar target/daily-task-orchestrator.jar > full_output.txt 2>&1

# Search for errors
grep -i "exception\|error\|failed" full_output.txt

# See last N lines
tail -100 full_output.txt
```

### Check What Gets Logged

Look in logs for these patterns to understand flow:

```
[INFO] Initializing Claude API client with provider: ANTHROPIC
[INFO] Using direct Anthropic API client
[DEBUG] Sending message to Claude...
[ERROR] ClaudeApiException occurred
[INFO] Retry attempt 1 of 3
```

---

## Getting Help

### Before Asking for Help

1. **Verify variables:**
   ```bash
   env | grep CLAUDE
   env | grep ANTHROPIC
   env | grep AWS
   ```

2. **Check logs:**
   ```bash
   # Run the app and capture output
   java -jar target/daily-task-orchestrator.jar 2>&1 | tee app.log
   ```

3. **Verify connectivity:**
   ```bash
   curl https://api.anthropic.com/v1/models
   ```

4. **Search existing issues:**
   - GitHub Issues
   - Documentation
   - Stack Overflow with tags

### Reporting an Issue

Include:
1. Error message (exact text)
2. Stack trace (if available)
3. Environment setup (CLAUDE_PROVIDER, AWS_REGION, etc. - NO keys!)
4. Steps to reproduce
5. What you expected vs. what happened
6. Your Java version: `java -version`
7. Maven version: `mvn -version`

**Example:**
```
Title: "Anthropic API returns 401 after key was working"

Error Message: ClaudeApiException{errorType=AUTHENTICATION_FAILED, 
statusCode=401}

Environment:
- CLAUDE_PROVIDER=ANTHROPIC
- CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
- Java: 21.0.1
- Maven: 3.8.1

Steps:
1. Set environment variables
2. Run: mvn clean package
3. Run: java -jar target/daily-task-orchestrator.jar

Expected: Application starts and summarizes tasks
Actual: Gets 401 error in logs

Note: API key was working yesterday
```

---

## Performance Troubleshooting

### "Requests are slow"

```bash
# 1. Check network latency
time curl https://api.anthropic.com/v1/models

# 2. Use faster model
export CLAUDE_MODEL_ID=claude-3-haiku-20240307  # Faster

# 3. Reduce max tokens
export CLAUDE_MAX_TOKENS=500  # Smaller response

# 4. Check if provider is slow
# Monitor provider status pages
```

### "High API costs"

```bash
# 1. Monitor usage
# Anthropic: https://console.anthropic.com/usage
# AWS: AWS Cost Explorer

# 2. Reduce max tokens (fewer tokens = less cost)
export CLAUDE_MAX_TOKENS=500

# 3. Use more economical model
export CLAUDE_MODEL_ID=claude-3-haiku-20240307

# 4. Implement caching (future feature)
# Avoid calling Claude for duplicate requests

# 5. Batch requests when possible
# Process multiple tasks in one call instead of many calls
```

---

## Checklist for Common Issues

Use this checklist to diagnose issues quickly:

- [ ] `CLAUDE_PROVIDER` is set and spelled correctly (ANTHROPIC or AWS_BEDROCK)
- [ ] `CLAUDE_MODEL_ID` is set and uses correct format for provider
- [ ] For Anthropic: `ANTHROPIC_API_KEY` is set and non-empty
- [ ] For AWS: `AWS_REGION` is set
- [ ] For AWS: AWS credentials are configured (check `aws sts get-caller-identity`)
- [ ] Network connectivity works (can ping provider endpoint)
- [ ] All numeric environment variables are actually numbers
- [ ] Temperature is between 0.0 and 1.0
- [ ] Max tokens is positive
- [ ] Logs show no obvious errors
- [ ] Provider status pages show no outages
- [ ] Code changes haven't broken existing tests

---

## Quick Reference

| Issue | Check | Solution |
|-------|-------|----------|
| "Provider required" | `echo $CLAUDE_PROVIDER` | Export ANTHROPIC or AWS_BEDROCK |
| "Auth failed" | Check API key/credentials | Verify in console, regenerate if needed |
| "Model not found" | Check model ID format | Use correct format for provider |
| "Timeout" | Increase timeout | `export CLAUDE_TIMEOUT_SECONDS=60` |
| "Rate limited" | Check usage limits | Reduce frequency or upgrade plan |
| "Malformed response" | Increase max tokens | `export CLAUDE_MAX_TOKENS=2000` |
| "Connection refused" | Ping provider | Check network/firewall |
| "Invalid config" | Check env vars | Verify types (int, double, etc.) |

