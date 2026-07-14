# AWS Lambda Deployment Guide

This guide covers deploying the Daily Task Orchestrator to AWS Lambda with automatic daily execution via EventBridge.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building the Lambda Package](#building-the-lambda-package)
3. [Setting Up AWS Secrets Manager](#setting-up-aws-secrets-manager)
4. [Initializing Gmail OAuth Tokens](#initializing-gmail-oauth-tokens)
5. [Creating IAM Role](#creating-iam-role)
6. [Deploying Lambda Function](#deploying-lambda-function)
7. [Configuring EventBridge Schedule](#configuring-eventbridge-schedule)
8. [Testing the Deployment](#testing-the-deployment)
9. [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
10. [Cost Optimization](#cost-optimization)

---

## Prerequisites

### Required Tools

- **Java 21** - Runtime for building and testing
- **Maven 3.6+** - Build tool
- **AWS CLI 2.x** - For AWS operations
- **SAM CLI** (optional) - For local testing
- **Valid AWS Account** with appropriate permissions

### AWS Permissions Required

Your AWS user/role needs:
- `lambda:*` - Lambda function management
- `iam:CreateRole`, `iam:AttachRolePolicy` - IAM role creation
- `secretsmanager:*` - Secrets Manager access
- `events:*` - EventBridge rule management
- `logs:*` - CloudWatch Logs access
- `bedrock:InvokeModel` - Claude AI via Bedrock

### Environment Variables (Local Development)

Before starting, ensure you have these for local token initialization:

```bash
export GMAIL_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GMAIL_CLIENT_SECRET="GOCSPX-your-secret"
export CLAUDE_PROVIDER="AWS_BEDROCK"
export CLAUDE_MODEL_ID="anthropic.claude-3-5-sonnet-20241022-v2:0"
export EMAIL_SMTP_HOST="smtp.gmail.com"
export EMAIL_SMTP_PORT="587"
export EMAIL_USERNAME="your-email@gmail.com"
export EMAIL_PASSWORD="your-gmail-app-password"
export EMAIL_FROM="your-email@gmail.com"
export EMAIL_TO="recipient@gmail.com"
```

---

## Building the Lambda Package

### 1. Build Fat JAR

```bash
cd daily-task-orchestrator
mvn clean package
```

**Expected Output:**
- JAR location: `target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar`
- Size: ~30-40 MB (well under Lambda's 50 MB direct upload limit)
- All dependencies included (fat JAR)

### 2. Verify Build

```bash
# Check JAR size
ls -lh target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar

# Verify Lambda handler class exists
jar tf target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar | grep DailyTaskLambdaHandler
```

Expected output:
```
com/dailytask/lambda/DailyTaskLambdaHandler.class
```

### 3. Test Locally (Optional)

Run unit tests:
```bash
mvn test -Dtest=DailyTaskLambdaHandlerTest
```

Test with SAM CLI (if installed):
```bash
sam local invoke DailyTaskFunction --event events/scheduled-event.json
```

---

## Setting Up AWS Secrets Manager

### 1. Create Application Configuration Secret

This secret stores all environment variables as JSON.

```bash
aws secretsmanager create-secret \
  --name daily-task-orchestrator/app-config \
  --description "Application configuration for Daily Task Orchestrator" \
  --secret-string '{
    "GMAIL_CLIENT_ID": "your-client-id.apps.googleusercontent.com",
    "GMAIL_CLIENT_SECRET": "GOCSPX-your-secret",
    "CLAUDE_PROVIDER": "AWS_BEDROCK",
    "CLAUDE_MODEL_ID": "anthropic.claude-3-5-sonnet-20241022-v2:0",
    "EMAIL_SMTP_HOST": "smtp.gmail.com",
    "EMAIL_SMTP_PORT": "587",
    "EMAIL_USERNAME": "your-email@gmail.com",
    "EMAIL_PASSWORD": "your-gmail-app-password",
    "EMAIL_FROM": "your-email@gmail.com",
    "EMAIL_TO": "recipient@gmail.com"
  }' \
  --region us-east-1
```

**Replace:**
- `your-client-id.apps.googleusercontent.com` - Your Gmail OAuth Client ID
- `GOCSPX-your-secret` - Your Gmail OAuth Client Secret
- `your-email@gmail.com` - Your Gmail address
- `your-gmail-app-password` - Gmail App Password (not regular password)
- `recipient@gmail.com` - Where to send daily summaries

### 2. Create Placeholder for Gmail Tokens

```bash
aws secretsmanager create-secret \
  --name daily-task-orchestrator/gmail-tokens \
  --description "OAuth tokens for Gmail API access" \
  --secret-string '{}' \
  --region us-east-1
```

**Note:** This will be populated in the next step with actual tokens.

### 3. Verify Secrets Created

```bash
aws secretsmanager list-secrets \
  --filters Key=name,Values=daily-task-orchestrator \
  --region us-east-1
```

Expected: 2 secrets listed.

---

## Initializing Gmail OAuth Tokens

Gmail API requires OAuth2 tokens. These must be initialized locally, then uploaded to Secrets Manager.

### 1. Run Local Authentication

```bash
# Set DEPLOYMENT_ENV to local (file-based token storage)
export DEPLOYMENT_ENV=local

# Run the application - this will open a browser for OAuth flow
java -jar target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar
```

**What Happens:**
1. Browser opens for Google OAuth consent
2. Sign in and grant permissions
3. Tokens saved to `~/.dailytask/gmail_tokens/StoredCredential`

### 2. Extract Token JSON

```bash
# Read the stored credential (it's JSON serialized)
cat ~/.dailytask/gmail_tokens/StoredCredential
```

Expected format (minified JSON):
```json
{"accessToken":"ya29...","refreshToken":"1//0g...","expirationTimeMilliseconds":1720...}
```

### 3. Upload Tokens to Secrets Manager

```bash
# Update the gmail-tokens secret with actual tokens
aws secretsmanager update-secret \
  --secret-id daily-task-orchestrator/gmail-tokens \
  --secret-string file://~/.dailytask/gmail_tokens/StoredCredential \
  --region us-east-1
```

### 4. Verify Token Upload

```bash
aws secretsmanager get-secret-value \
  --secret-id daily-task-orchestrator/gmail-tokens \
  --query SecretString \
  --output text \
  --region us-east-1
```

Expected: JSON with `accessToken`, `refreshToken`, `expirationTimeMilliseconds`.

---

## Creating IAM Role

Lambda needs an IAM role with permissions for Secrets Manager, Bedrock, and CloudWatch Logs.

### 1. Create Trust Policy

Save as `lambda-trust-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### 2. Create IAM Role

```bash
aws iam create-role \
  --role-name daily-task-orchestrator-lambda-role \
  --assume-role-policy-document file://lambda-trust-policy.json \
  --description "Execution role for Daily Task Orchestrator Lambda"
```

### 3. Attach Policies

#### CloudWatch Logs (for logging)

```bash
aws iam attach-role-policy \
  --role-name daily-task-orchestrator-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

#### Secrets Manager Access

Save as `secrets-manager-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret",
        "secretsmanager:UpdateSecret"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:daily-task-orchestrator/*"
    }
  ]
}
```

Create and attach:

```bash
aws iam create-policy \
  --policy-name DailyTaskOrchestratorSecretsAccess \
  --policy-document file://secrets-manager-policy.json

# Get your AWS Account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws iam attach-role-policy \
  --role-name daily-task-orchestrator-lambda-role \
  --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorSecretsAccess
```

#### Bedrock Access (for Claude AI)

Save as `bedrock-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel"
      ],
      "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude*"
    }
  ]
}
```

Create and attach:

```bash
aws iam create-policy \
  --policy-name DailyTaskOrchestratorBedrockAccess \
  --policy-document file://bedrock-policy.json

aws iam attach-role-policy \
  --role-name daily-task-orchestrator-lambda-role \
  --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorBedrockAccess
```

### 4. Wait for IAM Propagation

```bash
# IAM changes can take 10-30 seconds to propagate
sleep 15
```

---

## Deploying Lambda Function

### 1. Create Lambda Function

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws lambda create-function \
  --function-name daily-task-orchestrator \
  --runtime java21 \
  --role arn:aws:iam::${ACCOUNT_ID}:role/daily-task-orchestrator-lambda-role \
  --handler com.dailytask.lambda.DailyTaskLambdaHandler::handleRequest \
  --zip-file fileb://target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar \
  --timeout 300 \
  --memory-size 1024 \
  --environment Variables="{DEPLOYMENT_ENV=lambda,AWS_REGION=us-east-1}" \
  --region us-east-1
```

**Configuration Explained:**
- `--runtime java21` - Java 21 runtime (Corretto)
- `--timeout 300` - 5 minutes (Gmail + Claude + SMTP can take time)
- `--memory-size 1024` - 1 GB RAM (adequate for fat JAR)
- `DEPLOYMENT_ENV=lambda` - Triggers AWS Secrets Manager usage
- `AWS_REGION=us-east-1` - Required for Secrets Manager and Bedrock

### 2. Verify Deployment

```bash
aws lambda get-function --function-name daily-task-orchestrator
```

Expected: Function status `Active`.

### 3. Update Function (If Already Deployed)

To redeploy after code changes:

```bash
mvn clean package

aws lambda update-function-code \
  --function-name daily-task-orchestrator \
  --zip-file fileb://target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar \
  --region us-east-1
```

---

## Configuring EventBridge Schedule

Set up automatic daily execution at 9:00 AM UTC.

### 1. Create EventBridge Rule

```bash
aws events put-rule \
  --name daily-task-orchestrator-9am \
  --schedule-expression "cron(0 9 * * ? *)" \
  --state ENABLED \
  --description "Trigger Daily Task Orchestrator at 9:00 AM UTC daily" \
  --region us-east-1
```

**Cron Syntax:** `cron(Minutes Hours Day Month DayOfWeek Year)`
- `0 9 * * ? *` = Every day at 9:00 AM UTC
- For Poland (UTC+1 winter, UTC+2 summer):
  - Winter: `cron(0 8 * * ? *)` (8 AM UTC = 9 AM Poland)
  - Summer: `cron(0 7 * * ? *)` (7 AM UTC = 9 AM Poland)

### 2. Add Lambda as Target

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws events put-targets \
  --rule daily-task-orchestrator-9am \
  --targets "Id"="1","Arn"="arn:aws:lambda:us-east-1:${ACCOUNT_ID}:function:daily-task-orchestrator" \
  --region us-east-1
```

### 3. Grant EventBridge Permission to Invoke Lambda

```bash
aws lambda add-permission \
  --function-name daily-task-orchestrator \
  --statement-id AllowEventBridgeInvoke \
  --action lambda:InvokeFunction \
  --principal events.amazonaws.com \
  --source-arn arn:aws:events:us-east-1:${ACCOUNT_ID}:rule/daily-task-orchestrator-9am \
  --region us-east-1
```

### 4. Verify EventBridge Configuration

```bash
aws events describe-rule --name daily-task-orchestrator-9am --region us-east-1
aws events list-targets-by-rule --rule daily-task-orchestrator-9am --region us-east-1
```

---

## Testing the Deployment

### 1. Manual Invocation

Test Lambda function manually before waiting for scheduled trigger:

```bash
aws lambda invoke \
  --function-name daily-task-orchestrator \
  --payload '{"id":"manual-test"}' \
  response.json \
  --region us-east-1

# Check response
cat response.json
```

Expected response:
```json
"SUCCESS"
```

### 2. Check CloudWatch Logs

```bash
# Tail logs in real-time
aws logs tail /aws/lambda/daily-task-orchestrator --follow --region us-east-1
```

Expected log entries:
- `=== Lambda Invoked by EventBridge ===`
- `Created 1 data source(s)`
- `Created task summarizer (Claude integration)`
- `Orchestration completed successfully`

### 3. Verify Email Received

Check the recipient inbox (`EMAIL_TO` address) for the daily summary email.

### 4. Test Scheduled Execution

Wait for the scheduled time (9:00 AM UTC) or temporarily change the cron expression:

```bash
# Test in 5 minutes: cron(M H * * ? *) where M = current_minute + 5
aws events put-rule \
  --name daily-task-orchestrator-9am \
  --schedule-expression "cron(15 10 * * ? *)" \
  --state ENABLED \
  --region us-east-1
```

---

## Monitoring and Troubleshooting

### CloudWatch Logs

View recent logs:
```bash
aws logs tail /aws/lambda/daily-task-orchestrator --since 1h --region us-east-1
```

Filter for errors:
```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/daily-task-orchestrator \
  --filter-pattern "ERROR" \
  --region us-east-1
```

### Lambda Metrics

Check invocation metrics:
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=daily-task-orchestrator \
  --start-time $(date -u -d '1 day ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Sum \
  --region us-east-1
```

### Common Issues

#### 1. "Authentication failed" or "Access Denied"

**Cause:** IAM role missing permissions or Secrets Manager secrets not accessible.

**Fix:**
```bash
# Verify IAM role has all required policies
aws iam list-attached-role-policies --role-name daily-task-orchestrator-lambda-role

# Test Secrets Manager access
aws secretsmanager get-secret-value --secret-id daily-task-orchestrator/app-config
```

#### 2. "No OAuth tokens found in Secrets Manager"

**Cause:** Gmail tokens not initialized or not uploaded.

**Fix:** Re-run [Initializing Gmail OAuth Tokens](#initializing-gmail-oauth-tokens).

#### 3. "Task timeout" or "Function timed out"

**Cause:** 300s timeout insufficient (rare, but possible with slow network).

**Fix:**
```bash
aws lambda update-function-configuration \
  --function-name daily-task-orchestrator \
  --timeout 600 \
  --region us-east-1
```

#### 4. "Invalid model identifier" (Bedrock)

**Cause:** Model access not requested in Bedrock console.

**Fix:**
1. Go to AWS Console → Amazon Bedrock → Model access
2. Request access to **Anthropic Claude** models
3. Wait for approval (usually instant)

#### 5. EventBridge Not Triggering

**Check rule status:**
```bash
aws events describe-rule --name daily-task-orchestrator-9am
```

**Verify Lambda permission:**
```bash
aws lambda get-policy --function-name daily-task-orchestrator | jq -r '.Policy' | jq .
```

Should include a statement with `Principal: events.amazonaws.com`.

---

## Cost Optimization

### Current Estimated Monthly Cost

| Service | Usage | Cost |
|---------|-------|------|
| Lambda (1024 MB, 30s avg, 30 invocations) | Free tier | $0.00 |
| Secrets Manager (2 secrets) | $0.40/secret/month | $0.80 |
| Bedrock Claude 3.5 Sonnet (~45K tokens/month) | $3 input, $15 output per 1M | $0.32 |
| CloudWatch Logs (~1.5 MB/month) | Free tier | $0.00 |
| EventBridge (30 events/month) | Free tier | $0.00 |
| **Total** | | **~$1.12/month** |

### Optimization Tips

1. **Combine Secrets** (save $0.40/month)
   ```bash
   # Merge gmail-tokens into app-config JSON
   # Use structured secret parsing in code
   ```

2. **Use Claude Haiku** (save ~80% on AI costs)
   ```bash
   # Update CLAUDE_MODEL_ID to:
   CLAUDE_MODEL_ID="anthropic.claude-3-haiku-20240307-v1:0"
   ```

3. **Reduce Memory** (Lambda already in free tier, but good practice)
   ```bash
   aws lambda update-function-configuration \
     --function-name daily-task-orchestrator \
     --memory-size 768 \
     --region us-east-1
   ```

4. **Set Log Retention**
   ```bash
   aws logs put-retention-policy \
     --log-group-name /aws/lambda/daily-task-orchestrator \
     --retention-in-days 7 \
     --region us-east-1
   ```

**Optimized Annual Cost:** ~$8.64/year

---

## Next Steps

- **Phase 6e:** Full AWS deployment (manual steps above)
- **Phase 6f:** Set up CloudWatch alarms for error monitoring
- **Phase 7 (Future):** Web dashboard for viewing summaries
- **Phase 8 (Future):** Additional data sources (Slack, Jira, etc.)

---

## Cleanup (Undeployment)

To remove all AWS resources:

```bash
# Delete EventBridge rule
aws events remove-targets --rule daily-task-orchestrator-9am --ids 1
aws events delete-rule --name daily-task-orchestrator-9am

# Delete Lambda function
aws lambda delete-function --function-name daily-task-orchestrator

# Delete IAM role and policies
aws iam detach-role-policy --role-name daily-task-orchestrator-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam detach-role-policy --role-name daily-task-orchestrator-lambda-role --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorSecretsAccess
aws iam detach-role-policy --role-name daily-task-orchestrator-lambda-role --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorBedrockAccess
aws iam delete-role --role-name daily-task-orchestrator-lambda-role

aws iam delete-policy --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorSecretsAccess
aws iam delete-policy --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/DailyTaskOrchestratorBedrockAccess

# Delete Secrets Manager secrets
aws secretsmanager delete-secret --secret-id daily-task-orchestrator/app-config --force-delete-without-recovery
aws secretsmanager delete-secret --secret-id daily-task-orchestrator/gmail-tokens --force-delete-without-recovery
```

---

## References

- [AWS Lambda Java Runtime](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Amazon EventBridge](https://docs.aws.amazon.com/eventbridge/)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/)
