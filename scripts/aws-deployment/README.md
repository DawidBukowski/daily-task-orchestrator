# AWS Deployment Scripts - Phase 6e & 6f

Automated deployment scripts for Daily Task Orchestrator Lambda function with EventBridge scheduling.

## Prerequisites

1. **AWS CLI** installed and configured
   ```bash
   aws --version
   aws sts get-caller-identity
   ```

2. **Java 21** and **Maven 3.6+**
   ```bash
   java -version
   mvn -version
   ```

3. **Required Environment Variables**
   ```bash
   # Gmail API
   export GMAIL_CLIENT_ID="your-client-id"
   export GMAIL_CLIENT_SECRET="your-client-secret"
   
   # Email SMTP
   export EMAIL_SMTP_HOST="smtp.gmail.com"
   export EMAIL_SMTP_PORT="587"
   export EMAIL_USERNAME="your-email@gmail.com"
   export EMAIL_PASSWORD="your-gmail-app-password"
   export EMAIL_FROM="your-email@gmail.com"
   export EMAIL_TO="recipient@gmail.com"
   
   # Claude AI (optional - defaults to AWS Bedrock)
   export CLAUDE_PROVIDER="AWS_BEDROCK"  # or "ANTHROPIC"
   export CLAUDE_MODEL_ID="anthropic.claude-3-5-sonnet-20241022-v2:0"
   
   # AWS Region (optional - defaults to us-east-1)
   export AWS_REGION="us-east-1"
   ```

## Quick Start - Complete Deployment

Run the all-in-one deployment script:

```bash
cd /path/to/daily-task-orchestrator
./scripts/aws-deployment/deploy-all.sh
```

This will:
1. Build the project (`mvn clean package`)
2. Create IAM role with necessary permissions
3. Create AWS Secrets Manager secrets
4. Initialize Gmail OAuth tokens (opens browser)
5. Upload tokens to Secrets Manager
6. Deploy Lambda function
7. Test Lambda function execution

**Total time:** ~5-10 minutes (depending on Gmail volume)

## Complete Deployment with Scheduling

To deploy AND configure scheduled execution:

```bash
cd /path/to/daily-task-orchestrator
./scripts/aws-deployment/deploy-all.sh
./scripts/aws-deployment/setup-eventbridge.sh
```

This will deploy the Lambda function and configure it to run automatically every day at 9:00 AM UTC.

## Individual Scripts

If you prefer step-by-step execution or need to re-run specific steps:

### 1. Setup IAM Role

```bash
./scripts/aws-deployment/setup-iam.sh
```

Creates:
- IAM role: `daily-task-orchestrator-lambda-role`
- IAM policy: `daily-task-orchestrator-lambda-policy`
- Permissions: CloudWatch Logs, Secrets Manager, Bedrock

**Idempotent:** Can be run multiple times safely.

### 2. Setup Secrets Manager

```bash
./scripts/aws-deployment/setup-secrets.sh
```

Creates two secrets:
- `daily-task-orchestrator/app-config` - Application configuration
- `daily-task-orchestrator/gmail-tokens` - Gmail OAuth tokens (empty placeholder)

**Idempotent:** Updates existing secrets if already present.

### 3. Initialize Gmail Tokens

```bash
./scripts/aws-deployment/init-gmail-tokens.sh
```

- Runs the application locally with `DEPLOYMENT_ENV=local`
- Opens browser for Gmail OAuth flow
- Stores tokens in `tokens/StoredCredential`

**⚠ Browser interaction required**

### 4. Upload Tokens to Secrets Manager

```bash
./scripts/aws-deployment/upload-tokens.sh
```

Uploads the token file from `tokens/StoredCredential` to AWS Secrets Manager secret: `daily-task-orchestrator/gmail-tokens`

**Prerequisite:** Step 3 must be completed first.

### 5. Deploy Lambda Function

```bash
./scripts/aws-deployment/deploy-lambda.sh
```

- Creates or updates Lambda function: `daily-task-orchestrator`
- Runtime: Java 21
- Handler: `com.dailytask.lambda.DailyTaskLambdaHandler::handleRequest`
- Memory: 1024 MB
- Timeout: 300 seconds (5 minutes)
- Environment: `DEPLOYMENT_ENV=lambda`, `AWS_REGION=us-east-1`

**Idempotent:** Updates function code if already exists.

### 6. Test Lambda Function

```bash
./scripts/aws-deployment/test-lambda.sh
```

- Invokes Lambda with test EventBridge event
- Shows execution logs from CloudWatch
- Verifies email was sent

**Expected Output:** `"SUCCESS"` response and task summary email received.

### 7. Setup EventBridge Scheduling (Phase 6f)

```bash
./scripts/aws-deployment/setup-eventbridge.sh
```

- Creates EventBridge rule: `daily-task-orchestrator-9am`
- Schedule: `cron(0 9 * * ? *)` - Daily at 9:00 AM UTC
- Adds Lambda as target
- Grants EventBridge permission to invoke Lambda
- Configures CloudWatch log retention (30 days)

**Idempotent:** Updates existing rule if already present.

**Post-Setup:**
- Lambda runs automatically every day at 9:00 AM UTC
- Check email daily for task summary
- Monitor CloudWatch logs for execution status

## Troubleshooting

### IAM Role Issues

**Error:** Role does not exist
```bash
./scripts/aws-deployment/setup-iam.sh
```

**Error:** Insufficient permissions
- Ensure your AWS user has permissions to create IAM roles and policies
- Required actions: `iam:CreateRole`, `iam:CreatePolicy`, `iam:AttachRolePolicy`

### Secrets Manager Issues

**Error:** Secret already exists with different values
```bash
# Update existing secret
aws secretsmanager put-secret-value \
  --secret-id daily-task-orchestrator/app-config \
  --secret-string "$(cat /tmp/new-config.json)"
```

### Gmail Token Issues

**Error:** Token file not found after init
- Check for OAuth consent screen errors in terminal output
- Verify `GMAIL_CLIENT_ID` and `GMAIL_CLIENT_SECRET` are correct
- Ensure browser completed the OAuth flow successfully
- Re-run: `./scripts/aws-deployment/init-gmail-tokens.sh`

**Error:** Tokens uploaded but Lambda can't access Gmail
- Verify tokens are valid: run local test first
- Check token expiration (refresh tokens should be permanent)
- Re-initialize and upload tokens

### Lambda Deployment Issues

**Error:** JAR file not found
```bash
mvn clean package
```

**Error:** Role ARN not found
```bash
./scripts/aws-deployment/setup-iam.sh
```

**Error:** Code size exceeds limit
- Current JAR: ~30 MB (under 50 MB limit)
- If exceeds: use S3 bucket for deployment

### Lambda Execution Issues

**Error:** "Orchestration failed"
- Check CloudWatch logs: `/aws/lambda/daily-task-orchestrator`
- Common causes:
  - Missing/invalid secrets in Secrets Manager
  - Expired Gmail tokens → re-run token initialization
  - Invalid Claude configuration
  - SMTP authentication failure

**Error:** Timeout after 5 minutes
- Increase timeout in `deploy-lambda.sh` (max: 900 seconds)
- Optimize email volume or Claude processing

**Error:** Out of memory
- Increase memory in `deploy-lambda.sh` (current: 1024 MB)
- Recommended: 1536 MB for large email volumes

### EventBridge Scheduling Issues

**Error:** Rule exists but Lambda not triggered
- Verify rule is ENABLED:
  ```bash
  aws events describe-rule --name daily-task-orchestrator-9am --region us-east-1
  ```
- Check Lambda has EventBridge permission:
  ```bash
  aws lambda get-policy --function-name daily-task-orchestrator --region us-east-1
  ```
- Re-run setup-eventbridge.sh to fix permissions

**Error:** Need to change schedule time
- Edit `SCHEDULE_EXPRESSION` in `setup-eventbridge.sh`
- Re-run the script to update the rule
- Example schedules:
  - `cron(0 8 * * ? *)` - 8:00 AM UTC
  - `cron(30 12 * * ? *)` - 12:30 PM UTC
  - `cron(0 0 * * MON *)` - Monday at midnight UTC

**Want to disable scheduled execution temporarily:**
```bash
aws events disable-rule --name daily-task-orchestrator-9am --region us-east-1
```

**Want to re-enable:**
```bash
aws events enable-rule --name daily-task-orchestrator-9am --region us-east-1
```

## AWS Resources Created

| Resource | Name | Description |
|----------|------|-------------|
| IAM Role | `daily-task-orchestrator-lambda-role` | Lambda execution role |
| IAM Policy | `daily-task-orchestrator-lambda-policy` | CloudWatch, Secrets Manager, Bedrock permissions |
| Secret | `daily-task-orchestrator/app-config` | Application configuration (JSON) |
| Secret | `daily-task-orchestrator/gmail-tokens` | Gmail OAuth tokens |
| Lambda Function | `daily-task-orchestrator` | Main Lambda function |
| CloudWatch Log Group | `/aws/lambda/daily-task-orchestrator` | Lambda execution logs (30 day retention) |
| EventBridge Rule | `daily-task-orchestrator-9am` | Scheduled trigger (9:00 AM UTC daily) |

## Cost Estimate

**Monthly costs (30 daily executions):**
- Lambda: $0.00 (free tier)
- Secrets Manager: $0.80 (2 secrets × $0.40/month)
- Bedrock: ~$0.32 (Claude 3.5 Sonnet)
- CloudWatch Logs: $0.00 (free tier)
- **Total: ~$1.12/month (~$13/year)**

## Next Steps

After successful deployment:

1. **Configure EventBridge** for scheduled execution (Phase 6f) - if not already done
   ```bash
   ./scripts/aws-deployment/setup-eventbridge.sh
   ```

2. **Monitor CloudWatch Logs**
   ```bash
   aws logs tail /aws/lambda/daily-task-orchestrator --follow
   ```

3. **View resources in AWS Console**
   - Lambda: https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions/daily-task-orchestrator
   - EventBridge: https://console.aws.amazon.com/events/home?region=us-east-1#/eventbus/default/rules/daily-task-orchestrator-9am
   - CloudWatch: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups

4. **Verify first scheduled execution**
   - Wait for 9:00 AM UTC
   - Check email for task summary
   - Review CloudWatch logs for execution status

## Cleanup (Undeploy)

To remove all AWS resources:

```bash
# Delete EventBridge rule
aws events remove-targets --rule daily-task-orchestrator-9am --ids 1 --region us-east-1
aws events delete-rule --name daily-task-orchestrator-9am --region us-east-1

# Remove Lambda permission for EventBridge
aws lambda remove-permission \
  --function-name daily-task-orchestrator \
  --statement-id EventBridgeInvoke-daily-task-orchestrator-9am \
  --region us-east-1

# Delete Lambda function
aws lambda delete-function --function-name daily-task-orchestrator --region us-east-1

# Delete secrets
aws secretsmanager delete-secret --secret-id daily-task-orchestrator/app-config --force-delete-without-recovery --region us-east-1
aws secretsmanager delete-secret --secret-id daily-task-orchestrator/gmail-tokens --force-delete-without-recovery --region us-east-1

# Detach and delete IAM policy
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws iam detach-role-policy \
  --role-name daily-task-orchestrator-lambda-role \
  --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/daily-task-orchestrator-lambda-policy
aws iam delete-policy --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/daily-task-orchestrator-lambda-policy

# Delete IAM role
aws iam delete-role --role-name daily-task-orchestrator-lambda-role

# Delete CloudWatch log group
aws logs delete-log-group --log-group-name /aws/lambda/daily-task-orchestrator --region us-east-1
```

## Support

For issues or questions:
- Check CloudWatch logs for detailed error messages
- Review [DEPLOYMENT_GUIDE.md](../../docs/06_AWS_Deployment/DEPLOYMENT_GUIDE.md) for detailed documentation
- Verify all environment variables are set correctly
- Ensure AWS credentials have sufficient permissions
