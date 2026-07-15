# Production Readiness Review

## Security Audit

### Checklist

- [x] No secrets in source code (`.gitignore` covers `.env`, `tokens/`, `StoredCredential`)
- [x] IAM role uses principle of least privilege (only Secrets Manager, Bedrock, CloudWatch)
- [x] Secrets Manager secrets encrypted at rest (AWS default KMS encryption)
- [x] No sensitive data in Lambda environment variables (only `DEPLOYMENT_ENV` and `AWS_REGION`)
- [x] CloudWatch logs do not contain API keys (verified: only masked/partial values logged)
- [x] OAuth tokens stored in Secrets Manager, not in Lambda code or environment
- [x] HTTPS enforced for all external API calls (Gmail, Bedrock, SMTP with TLS)

### IAM Permissions Summary

| Permission | Purpose | Resource Scope |
|-----------|---------|----------------|
| `secretsmanager:GetSecretValue` | Read app config and Gmail tokens | `daily-task-orchestrator/*` |
| `secretsmanager:UpdateSecret` | Persist refreshed OAuth tokens | `daily-task-orchestrator/gmail-tokens/*` |
| `bedrock:InvokeModel` | Claude AI summarization | `anthropic.claude*` models |
| `logs:CreateLogGroup` | CloudWatch logging | `/aws/lambda/daily-task-orchestrator` |
| `logs:CreateLogStream` | CloudWatch logging | Same as above |
| `logs:PutLogEvents` | CloudWatch logging | Same as above |

---

## Cost Validation

### Monthly Breakdown

| Service | Usage | Cost |
|---------|-------|------|
| AWS Lambda | 30 invocations/month, ~15s each, 1024 MB | $0.00 (free tier: 1M requests/month) |
| AWS Secrets Manager | 2 secrets, ~60 reads/month | $0.80 |
| AWS Bedrock (Claude 3.5 Sonnet) | ~30 requests, ~2K tokens each | ~$0.32 |
| CloudWatch Logs | ~1.5 MB/month, 30-day retention | $0.00 (free tier: 5 GB/month) |
| EventBridge | 30 invocations/month | $0.00 (free tier: 14M/month) |
| **Total** | | **~$1.12/month (~$13/year)** |

### Cost Notes
- Lambda free tier is **permanent** (not 12-month limited)
- Secrets Manager is the primary cost ($0.40/secret/month)
- Bedrock cost scales with email volume (more emails = more tokens)
- No data transfer costs (all within same region)

### Budget Recommendation

Set an AWS Budget alert at **$5/month** to catch anomalies:
```bash
aws budgets create-budget \
    --account-id YOUR_ACCOUNT_ID \
    --budget '{
        "BudgetName": "daily-task-orchestrator",
        "BudgetLimit": {"Amount": "5", "Unit": "USD"},
        "TimeUnit": "MONTHLY",
        "BudgetType": "COST",
        "CostFilters": {}
    }' \
    --notifications-with-subscribers '[{
        "Notification": {
            "NotificationType": "ACTUAL",
            "ComparisonOperator": "GREATER_THAN",
            "Threshold": 80,
            "ThresholdType": "PERCENTAGE"
        },
        "Subscribers": [{
            "SubscriptionType": "EMAIL",
            "Address": "YOUR_EMAIL"
        }]
    }]'
```

---

## Monitoring Recommendations

### CloudWatch Alarm: Lambda Errors

Alert on any Lambda failure (should be 0 errors in normal operation):

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name daily-task-orchestrator-errors \
    --alarm-description "Alert when daily task orchestrator Lambda fails" \
    --namespace AWS/Lambda \
    --metric-name Errors \
    --dimensions Name=FunctionName,Value=daily-task-orchestrator \
    --statistic Sum \
    --period 86400 \
    --evaluation-periods 1 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:YOUR_TOPIC \
    --region us-east-1
```

### CloudWatch Alarm: Lambda Duration

Alert when execution approaches timeout (80% of 300s = 240s):

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name daily-task-orchestrator-duration \
    --alarm-description "Alert when execution time exceeds 240 seconds" \
    --namespace AWS/Lambda \
    --metric-name Duration \
    --dimensions Name=FunctionName,Value=daily-task-orchestrator \
    --statistic Maximum \
    --period 86400 \
    --evaluation-periods 1 \
    --threshold 240000 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:YOUR_TOPIC \
    --region us-east-1
```

### SNS Topic for Notifications

Create an SNS topic before setting up alarms:

```bash
# Create topic
aws sns create-topic --name daily-task-orchestrator-alerts --region us-east-1

# Subscribe your email
aws sns subscribe \
    --topic-arn arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:daily-task-orchestrator-alerts \
    --protocol email \
    --notification-endpoint YOUR_EMAIL \
    --region us-east-1
```

---

## Backup & Recovery

| Component | Backup Strategy | Recovery Procedure |
|-----------|----------------|-------------------|
| Lambda code | Git repository | `./scripts/aws-deployment/deploy-lambda.sh` |
| App config secret | Secrets Manager versioning (automatic) | Restore previous version via AWS Console |
| Gmail tokens | Re-initializable locally | `init-gmail-tokens.sh` + `upload-tokens.sh` |
| IAM role/policies | Script-defined | `./scripts/aws-deployment/setup-iam.sh` |
| EventBridge rule | Script-defined | `./scripts/aws-deployment/setup-eventbridge.sh` |
| CloudWatch logs | 30-day retention | Not recoverable after expiry (non-critical) |

**Key insight:** The system is fully stateless. No persistent data is stored in Lambda. All state (tokens, config) is in Secrets Manager with automatic versioning. Full re-deployment from scratch takes ~5 minutes using `deploy-all.sh`.

---

## Maintenance Schedule

| Frequency | Task | How |
|-----------|------|-----|
| Daily | Check email arrived | Inbox |
| Weekly | Review CloudWatch metrics | AWS Console → Lambda → Monitoring |
| Monthly | Check for warnings in logs | `aws logs filter-log-events ... --filter-pattern "WARN"` |
| Quarterly | Verify Gmail token still works | Check for auth errors in logs |
| Quarterly | Review AWS costs | AWS Cost Explorer |
| Annually | Update Maven dependencies | `mvn versions:display-dependency-updates` |
| As-needed | Update Claude model ID | Update `CLAUDE_MODEL_ID` in Secrets Manager when new versions available |
| As-needed | Renew Gmail App Password | If Google disables it or you change password |

---

## Known Limitations

1. **No retry mechanism** — if Lambda fails, it fails until next day's scheduled run
2. **Single region** — no multi-region redundancy (acceptable for personal use)
3. **No streaming** — Claude response is synchronous (acceptable for daily batch)
4. **Token refresh requires local machine** — if refresh token is revoked, need browser access for re-auth
5. **No dead letter queue** — failed invocations are only logged, not queued for retry
