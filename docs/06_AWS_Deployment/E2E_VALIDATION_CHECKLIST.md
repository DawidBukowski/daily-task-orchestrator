# End-to-End Validation Checklist

Use this checklist after deployment or when verifying that the system works correctly.

---

## 1. Pre-Deployment Validation (Local)

- [ ] `mvn clean package` succeeds without errors
- [ ] `mvn test` passes all unit tests
- [ ] JAR file exists: `target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar`
- [ ] All required environment variables documented and ready:
  - `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`
  - `EMAIL_*` variables (SMTP config)
  - `CLAUDE_PROVIDER`, `CLAUDE_MODEL_ID`
  - `AWS_REGION`

---

## 2. Infrastructure Validation

### AWS Identity
- [ ] AWS CLI configured and authenticated:
  ```bash
  aws sts get-caller-identity
  ```
  Expected: Returns account ID, ARN, user ID

### IAM Role
- [ ] Lambda execution role exists:
  ```bash
  aws iam get-role --role-name daily-task-orchestrator-lambda-role
  ```
- [ ] Role has required policies attached:
  ```bash
  aws iam list-attached-role-policies --role-name daily-task-orchestrator-lambda-role
  ```
  Expected policies: Secrets Manager, Bedrock, CloudWatch Logs

### Secrets Manager
- [ ] App config secret exists and has content:
  ```bash
  aws secretsmanager get-secret-value \
      --secret-id daily-task-orchestrator/app-config \
      --region us-east-1 \
      --query 'SecretString' --output text | jq 'keys'
  ```
  Expected keys: `CLAUDE_PROVIDER`, `CLAUDE_MODEL_ID`, `EMAIL_*`, `GMAIL_*`

- [ ] Gmail tokens secret exists:
  ```bash
  aws secretsmanager describe-secret \
      --secret-id daily-task-orchestrator/gmail-tokens \
      --region us-east-1
  ```

### Lambda Function
- [ ] Lambda function exists and is configured:
  ```bash
  aws lambda get-function-configuration \
      --function-name daily-task-orchestrator \
      --region us-east-1 \
      --query '{Runtime: Runtime, Handler: Handler, Timeout: Timeout, MemorySize: MemorySize, Environment: Environment.Variables}'
  ```
  Expected: Runtime=java21, Timeout>=300, Memory>=1024, DEPLOYMENT_ENV=lambda

### EventBridge
- [ ] EventBridge rule exists and is ENABLED:
  ```bash
  aws events describe-rule \
      --name daily-task-orchestrator-9am \
      --region us-east-1 \
      --query '{State: State, ScheduleExpression: ScheduleExpression}'
  ```
  Expected: State=ENABLED, ScheduleExpression=`cron(0 9 * * ? *)`

- [ ] Rule targets the Lambda function:
  ```bash
  aws events list-targets-by-rule \
      --rule daily-task-orchestrator-9am \
      --region us-east-1 \
      --query 'Targets[].Arn'
  ```

- [ ] Lambda has EventBridge invoke permission:
  ```bash
  aws lambda get-policy \
      --function-name daily-task-orchestrator \
      --region us-east-1
  ```

---

## 3. Manual Invocation Test

- [ ] Invoke Lambda manually:
  ```bash
  ./scripts/aws-deployment/test-lambda.sh
  ```
  Or directly:
  ```bash
  aws lambda invoke \
      --function-name daily-task-orchestrator \
      --payload '{"source": "manual-test", "detail-type": "Manual Test"}' \
      --region us-east-1 \
      output.json && cat output.json
  ```
  Expected output: `"SUCCESS"`

- [ ] CloudWatch logs show full execution flow:
  ```bash
  aws logs filter-log-events \
      --log-group-name /aws/lambda/daily-task-orchestrator \
      --start-time $(date -d '5 minutes ago' +%s000) \
      --region us-east-1 \
      --query 'events[].message' --output text
  ```
  Expected log sequence:
  - [ ] `=== Lambda Invoked by EventBridge ===`
  - [ ] `Initializing components via AppConfig factories...`
  - [ ] `Created X data source(s)`
  - [ ] `Created task summarizer (Claude integration)`
  - [ ] `Created task notifier (email)`
  - [ ] `Starting orchestration...`
  - [ ] `Orchestration completed successfully in X ms`

- [ ] Email received at configured `EMAIL_TO` address
- [ ] Email contains:
  - [ ] Subject with task count
  - [ ] Priority color-coded tasks
  - [ ] AI recommendations section
  - [ ] Deadline information (if applicable)

---

## 4. Scheduled Execution Validation

- [ ] Confirm EventBridge rule is ENABLED (from Section 2)
- [ ] Wait for next 9:00 AM UTC execution (or temporarily modify schedule for testing):
  ```bash
  # Temporarily set to 5 minutes from now for testing (optional)
  # aws events put-rule --name daily-task-orchestrator-9am \
  #     --schedule-expression "cron(MINUTE HOUR * * ? *)" --region us-east-1
  ```
- [ ] After scheduled time, verify CloudWatch logs show invocation:
  ```bash
  aws logs filter-log-events \
      --log-group-name /aws/lambda/daily-task-orchestrator \
      --filter-pattern "Lambda Invoked by EventBridge" \
      --start-time $(date -d 'today 09:00 UTC' +%s000) \
      --region us-east-1
  ```
- [ ] Email received without manual trigger
- [ ] Restore original schedule if modified

---

## 5. Failure Recovery Test (Optional)

These tests confirm the system fails gracefully and recovers.

### 5a. Invalid secret test
- [ ] Temporarily rename app-config secret (or change a key to invalid value)
- [ ] Invoke Lambda manually
- [ ] Confirm Lambda fails with clear error message in logs
- [ ] Confirm Lambda execution is marked as failed (non-zero exit)
- [ ] Restore original secret value
- [ ] Invoke again — confirm recovery and "SUCCESS" response

### 5b. EventBridge disable test
- [ ] Disable rule:
  ```bash
  aws events disable-rule --name daily-task-orchestrator-9am --region us-east-1
  ```
- [ ] Wait past scheduled time — confirm no invocation
- [ ] Re-enable rule:
  ```bash
  aws events enable-rule --name daily-task-orchestrator-9am --region us-east-1
  ```

---

## 6. Sign-Off

| Item | Value |
|------|-------|
| Date of validation | ________ |
| Validated by | ________ |
| All checks passed | Yes / No |
| Manual invocation result | SUCCESS / FAILED |
| Scheduled invocation verified | Yes / No / Skipped |
| Notes | ________ |

---

## Troubleshooting

If any check fails, refer to [TROUBLESHOOTING_RUNBOOK.md](TROUBLESHOOTING_RUNBOOK.md) for diagnosis and remediation steps.
