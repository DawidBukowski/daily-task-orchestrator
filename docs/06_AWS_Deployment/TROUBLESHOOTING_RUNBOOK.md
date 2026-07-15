# Troubleshooting Runbook — AWS Deployment

## Quick Diagnostic Flowchart

```
Did the daily email arrive?
│
├── YES → System working correctly ✓
│
└── NO
    │
    ├── Are there CloudWatch logs from today (9:00 AM UTC)?
    │   │
    │   ├── NO → EventBridge or Lambda issue
    │   │   ├── Is the EventBridge rule ENABLED?
    │   │   │   └── Check: aws events describe-rule --name daily-task-orchestrator-9am
    │   │   ├── Does Lambda have invoke permission from EventBridge?
    │   │   │   └── Check: aws lambda get-policy --function-name daily-task-orchestrator
    │   │   └── Was Lambda deleted or renamed?
    │   │       └── Check: aws lambda get-function --function-name daily-task-orchestrator
    │   │
    │   └── YES → Lambda was invoked. Check log content:
    │       │
    │       ├── "Orchestration failed" in logs?
    │       │   ├── "Secret ... not found" → Secrets Manager issue (Scenario 2)
    │       │   ├── "No OAuth tokens found" → Gmail token issue (Scenario 3)
    │       │   ├── "Authentication failed" → Gmail refresh token expired (Scenario 3)
    │       │   ├── "ClaudeApiException" → Bedrock/Claude issue (Scenario 4)
    │       │   └── "Email sent" NOT in logs → SMTP issue (Scenario 5)
    │       │
    │       └── "SUCCESS" in logs but no email?
    │           └── Check spam folder, verify EMAIL_TO address
```

---

## Scenario 1: No Logs, No Invocation

**Symptoms:** No CloudWatch log entries at expected time, no email received.

**Possible Causes:**
- EventBridge rule is DISABLED
- Lambda function deleted or renamed
- Lambda invoke permission missing

**Diagnosis:**
```bash
# Check EventBridge rule status
aws events describe-rule --name daily-task-orchestrator-9am --region us-east-1

# Check rule targets
aws events list-targets-by-rule --rule daily-task-orchestrator-9am --region us-east-1

# Check Lambda exists
aws lambda get-function --function-name daily-task-orchestrator --region us-east-1

# Check Lambda resource policy (EventBridge permission)
aws lambda get-policy --function-name daily-task-orchestrator --region us-east-1
```

**Remediation:**
```bash
# Re-enable rule
aws events enable-rule --name daily-task-orchestrator-9am --region us-east-1

# Or re-run full EventBridge setup
./scripts/aws-deployment/setup-eventbridge.sh
```

---

## Scenario 2: Lambda Fails — Secrets Manager Error

**Symptoms:** Lambda invoked but fails immediately. Logs show secret retrieval errors.

**Log patterns:**
```
Secret 'daily-task-orchestrator/app-config' not found in Secrets Manager
Failed to retrieve or parse structured secret
SecretsException: Secret '...' has empty value
```

**Diagnosis:**
```bash
# Check if app-config secret exists
aws secretsmanager describe-secret \
    --secret-id daily-task-orchestrator/app-config \
    --region us-east-1

# Check if gmail-tokens secret exists
aws secretsmanager describe-secret \
    --secret-id daily-task-orchestrator/gmail-tokens \
    --region us-east-1

# Verify secret has content (careful: displays sensitive data)
aws secretsmanager get-secret-value \
    --secret-id daily-task-orchestrator/app-config \
    --region us-east-1 \
    --query 'SecretString' --output text | jq 'keys'
```

**Remediation:**
```bash
# Re-create secrets
./scripts/aws-deployment/setup-secrets.sh

# If only tokens are missing
./scripts/aws-deployment/upload-tokens.sh
```

---

## Scenario 3: Lambda Fails — Gmail Authentication Error

**Symptoms:** Lambda starts but fails during Gmail data source initialization.

**Log patterns:**
```
No OAuth tokens found in Secrets Manager. Initialize tokens locally first
Authentication failed due to network or IO error
Failed to load tokens from Secrets Manager
```

**Possible Causes:**
- Refresh token revoked (user changed password, revoked app access)
- Token secret deleted or corrupted
- App in "Testing" status (tokens expire after 7 days)
- IAM role missing `secretsmanager:UpdateSecret` permission (can't save refreshed token)

**Diagnosis:**
```bash
# Check if token secret exists and has content
aws secretsmanager get-secret-value \
    --secret-id daily-task-orchestrator/gmail-tokens/StoredCredential \
    --region us-east-1 \
    --query 'SecretString' --output text | jq 'keys'

# Check IAM role permissions
aws iam list-attached-role-policies \
    --role-name daily-task-orchestrator-lambda-role

# Check Google Cloud Console: is app in "Testing" or "In production"?
```

**Remediation:**
```bash
# Re-initialize tokens (requires browser access)
./scripts/aws-deployment/init-gmail-tokens.sh

# Upload new tokens
./scripts/aws-deployment/upload-tokens.sh

# Test
./scripts/aws-deployment/test-lambda.sh
```

See [GMAIL_TOKEN_WORKFLOW.md](GMAIL_TOKEN_WORKFLOW.md) for full token lifecycle details.

---

## Scenario 4: Lambda Fails — Claude/Bedrock Error

**Symptoms:** Lambda retrieves emails successfully but fails during AI summarization.

**Log patterns:**
```
ClaudeApiException
Authentication failed (Bedrock)
Model not found
The provided model identifier is invalid
Service quota exceeded
```

**Possible Causes:**
- Bedrock model access revoked or not approved
- Model ID incorrect or deprecated
- IAM role missing `bedrock:InvokeModel` permission
- Region doesn't support the specified model
- Request timeout (large email volume)

**Diagnosis:**
```bash
# Check model access
aws bedrock list-foundation-models \
    --region us-east-1 \
    --query "modelSummaries[?contains(modelId,'claude')].[modelId,modelLifecycle.status]" \
    --output table

# Check IAM permissions for Bedrock
aws iam simulate-principal-policy \
    --policy-source-arn arn:aws:iam::ACCOUNT_ID:role/daily-task-orchestrator-lambda-role \
    --action-names bedrock:InvokeModel \
    --resource-arns "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude*"

# Verify model ID in app-config secret
aws secretsmanager get-secret-value \
    --secret-id daily-task-orchestrator/app-config \
    --region us-east-1 \
    --query 'SecretString' --output text | jq '.CLAUDE_MODEL_ID'
```

**Remediation:**
- Request model access in AWS Console → Bedrock → Model access
- Update `CLAUDE_MODEL_ID` in Secrets Manager if model deprecated
- Increase `CLAUDE_TIMEOUT_SECONDS` in app-config secret if timeout

---

## Scenario 5: Lambda Fails — Email Sending Error

**Symptoms:** Orchestration runs, summarization succeeds, but email is not sent.

**Log patterns:**
```
Email notification sent successfully    ← NOT present
Orchestration failed                   ← after summarization logs
javax.mail.AuthenticationFailedException
Could not connect to SMTP host
```

**Possible Causes:**
- Gmail App Password expired or revoked
- 2FA disabled on Gmail account (App Passwords require 2FA)
- SMTP port blocked by Lambda network
- EMAIL_TO or EMAIL_FROM misconfigured

**Diagnosis:**
```bash
# Check email config in secrets
aws secretsmanager get-secret-value \
    --secret-id daily-task-orchestrator/app-config \
    --region us-east-1 \
    --query 'SecretString' --output text | jq '{EMAIL_SMTP_HOST, EMAIL_SMTP_PORT, EMAIL_USERNAME, EMAIL_FROM, EMAIL_TO}'
```

**Remediation:**
1. Generate new Gmail App Password (Google Account → Security → App Passwords)
2. Update `EMAIL_PASSWORD` in Secrets Manager:
   ```bash
   # Get current config
   CONFIG=$(aws secretsmanager get-secret-value \
       --secret-id daily-task-orchestrator/app-config \
       --region us-east-1 \
       --query 'SecretString' --output text)

   # Update password (use jq to modify)
   UPDATED=$(echo "$CONFIG" | jq '.EMAIL_PASSWORD = "new-app-password"')

   # Save back
   aws secretsmanager put-secret-value \
       --secret-id daily-task-orchestrator/app-config \
       --secret-string "$UPDATED" \
       --region us-east-1
   ```

---

## Scenario 6: Lambda Timeout

**Symptoms:** Lambda execution reaches the configured timeout limit.

**Log patterns:**
```
Remaining Time: XXX ms     ← initial remaining time
Task timed out after X seconds
```

**Possible Causes:**
- Large volume of emails to process
- Slow Bedrock response (model overloaded)
- Network issues between Lambda and external services

**Diagnosis:**
```bash
# Check Lambda timeout configuration
aws lambda get-function-configuration \
    --function-name daily-task-orchestrator \
    --region us-east-1 \
    --query '{Timeout: Timeout, MemorySize: MemorySize}'

# Check recent invocation durations
aws logs filter-log-events \
    --log-group-name /aws/lambda/daily-task-orchestrator \
    --filter-pattern "completed successfully in" \
    --start-time $(date -d '7 days ago' +%s000) \
    --region us-east-1
```

**Remediation:**
```bash
# Increase timeout (max 900 seconds = 15 minutes)
aws lambda update-function-configuration \
    --function-name daily-task-orchestrator \
    --timeout 600 \
    --region us-east-1

# Increase CLAUDE_TIMEOUT_SECONDS in app-config if Bedrock is slow
```

---

## Scenario 7: Lambda Out of Memory

**Symptoms:** Lambda killed by runtime, no graceful error message.

**Log patterns:**
```
Runtime exited with error: signal: killed
REPORT ... Max Memory Used: 1024 MB
```

**Remediation:**
```bash
# Increase memory (current: 1024 MB)
aws lambda update-function-configuration \
    --function-name daily-task-orchestrator \
    --memory-size 2048 \
    --region us-east-1
```

---

## Useful AWS CLI Commands

### View Recent Logs
```bash
# Last 30 minutes of logs
aws logs filter-log-events \
    --log-group-name /aws/lambda/daily-task-orchestrator \
    --start-time $(date -d '30 minutes ago' +%s000) \
    --region us-east-1 \
    --query 'events[].message' --output text

# Filter for errors only
aws logs filter-log-events \
    --log-group-name /aws/lambda/daily-task-orchestrator \
    --filter-pattern "ERROR" \
    --start-time $(date -d '24 hours ago' +%s000) \
    --region us-east-1
```

### Manual Lambda Invocation
```bash
# Trigger manually (same as test-lambda.sh)
aws lambda invoke \
    --function-name daily-task-orchestrator \
    --payload '{"source": "manual-test", "detail-type": "Manual Test"}' \
    --region us-east-1 \
    output.json

cat output.json
```

### Check Lambda Metrics
```bash
# Invocation count last 7 days
aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Invocations \
    --dimensions Name=FunctionName,Value=daily-task-orchestrator \
    --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 86400 \
    --statistics Sum \
    --region us-east-1

# Error count last 7 days
aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Errors \
    --dimensions Name=FunctionName,Value=daily-task-orchestrator \
    --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 86400 \
    --statistics Sum \
    --region us-east-1
```

---

## Key Log Patterns to Search

| Pattern | Meaning |
|---------|---------|
| `=== Lambda Invoked by EventBridge ===` | Lambda started successfully |
| `Initializing components via AppConfig factories` | Component setup beginning |
| `Created X data source(s)` | Gmail connection initialized |
| `Created task summarizer (Claude integration)` | Bedrock client ready |
| `Starting orchestration...` | Main workflow starting |
| `Orchestration completed successfully in X ms` | Full success |
| `Orchestration failed` | Fatal error — check exception below this line |
| `No OAuth tokens found in Secrets Manager` | Token initialization needed |
| `Token expired, refreshing...` | Normal token refresh (not an error) |
| `Successfully loaded OAuth credentials` | Token loaded OK |
