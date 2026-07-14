#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6f: EventBridge Scheduling${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Configuration
RULE_NAME="daily-task-orchestrator-9am"
FUNCTION_NAME="daily-task-orchestrator"
AWS_REGION="${AWS_REGION:-us-east-1}"
SCHEDULE_EXPRESSION="cron(0 9 * * ? *)"
LOG_GROUP="/aws/lambda/$FUNCTION_NAME"
LOG_RETENTION_DAYS=30

# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo -e "${YELLOW}Configuration:${NC}"
echo -e "  Rule Name: $RULE_NAME"
echo -e "  Schedule: Daily at 9:00 AM UTC"
echo -e "  Target Lambda: $FUNCTION_NAME"
echo -e "  Region: $AWS_REGION"
echo -e "  Log Retention: $LOG_RETENTION_DAYS days"
echo ""

# Step 1: Create or update EventBridge rule
echo -e "${YELLOW}[1/5] Creating EventBridge rule...${NC}"

EXISTING_RULE=$(aws events describe-rule \
    --name "$RULE_NAME" \
    --region "$AWS_REGION" 2>/dev/null || echo "")

if [ -z "$EXISTING_RULE" ]; then
    aws events put-rule \
        --name "$RULE_NAME" \
        --schedule-expression "$SCHEDULE_EXPRESSION" \
        --state ENABLED \
        --description "Daily Task Orchestrator - runs at 9:00 AM UTC daily" \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}✓ EventBridge rule created${NC}"
else
    aws events put-rule \
        --name "$RULE_NAME" \
        --schedule-expression "$SCHEDULE_EXPRESSION" \
        --state ENABLED \
        --description "Daily Task Orchestrator - runs at 9:00 AM UTC daily" \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}✓ EventBridge rule updated${NC}"
fi
echo ""

# Step 2: Add Lambda function as target
echo -e "${YELLOW}[2/5] Adding Lambda function as target...${NC}"

# Get Lambda function ARN
LAMBDA_ARN=$(aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --region "$AWS_REGION" \
    --query 'Configuration.FunctionArn' \
    --output text)

if [ -z "$LAMBDA_ARN" ]; then
    echo -e "${RED}✗ Lambda function not found: $FUNCTION_NAME${NC}"
    echo -e "${RED}Please run deploy-lambda.sh first${NC}"
    exit 1
fi

# Check if target already exists
EXISTING_TARGETS=$(aws events list-targets-by-rule \
    --rule "$RULE_NAME" \
    --region "$AWS_REGION" \
    --query 'Targets[?Id==`1`]' \
    --output text 2>/dev/null || echo "")

if [ -z "$EXISTING_TARGETS" ]; then
    aws events put-targets \
        --rule "$RULE_NAME" \
        --targets "Id=1,Arn=$LAMBDA_ARN" \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}✓ Lambda target added to rule${NC}"
else
    aws events put-targets \
        --rule "$RULE_NAME" \
        --targets "Id=1,Arn=$LAMBDA_ARN" \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}✓ Lambda target updated${NC}"
fi
echo ""

# Step 3: Grant EventBridge permission to invoke Lambda
echo -e "${YELLOW}[3/5] Granting EventBridge permission to invoke Lambda...${NC}"

STATEMENT_ID="EventBridgeInvoke-${RULE_NAME}"

# Remove existing permission if present (to update it)
aws lambda remove-permission \
    --function-name "$FUNCTION_NAME" \
    --statement-id "$STATEMENT_ID" \
    --region "$AWS_REGION" 2>/dev/null || true

# Add permission
aws lambda add-permission \
    --function-name "$FUNCTION_NAME" \
    --statement-id "$STATEMENT_ID" \
    --action "lambda:InvokeFunction" \
    --principal events.amazonaws.com \
    --source-arn "arn:aws:events:${AWS_REGION}:${ACCOUNT_ID}:rule/${RULE_NAME}" \
    --region "$AWS_REGION" > /dev/null

echo -e "${GREEN}✓ Permission granted${NC}"
echo ""

# Step 4: Configure CloudWatch log retention
echo -e "${YELLOW}[4/5] Configuring CloudWatch log retention...${NC}"

# Check if log group exists
LOG_GROUP_EXISTS=$(aws logs describe-log-groups \
    --log-group-name-prefix "$LOG_GROUP" \
    --region "$AWS_REGION" \
    --query "logGroups[?logGroupName=='$LOG_GROUP'].logGroupName" \
    --output text 2>/dev/null || echo "")

if [ -n "$LOG_GROUP_EXISTS" ]; then
    aws logs put-retention-policy \
        --log-group-name "$LOG_GROUP" \
        --retention-in-days "$LOG_RETENTION_DAYS" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Log retention set to $LOG_RETENTION_DAYS days${NC}"
else
    echo -e "${YELLOW}⚠ Log group doesn't exist yet (will be created on first Lambda execution)${NC}"
    echo -e "${YELLOW}  Run this script again after first execution to set retention${NC}"
fi
echo ""

# Step 5: Verify configuration
echo -e "${YELLOW}[5/5] Verifying configuration...${NC}"

RULE_STATE=$(aws events describe-rule \
    --name "$RULE_NAME" \
    --region "$AWS_REGION" \
    --query 'State' \
    --output text)

TARGETS_COUNT=$(aws events list-targets-by-rule \
    --rule "$RULE_NAME" \
    --region "$AWS_REGION" \
    --query 'length(Targets)' \
    --output text)

if [ "$RULE_STATE" == "ENABLED" ] && [ "$TARGETS_COUNT" -ge 1 ]; then
    echo -e "${GREEN}✓ Configuration verified successfully${NC}"
else
    echo -e "${RED}✗ Configuration verification failed${NC}"
    echo -e "${RED}  Rule State: $RULE_STATE (expected: ENABLED)${NC}"
    echo -e "${RED}  Targets Count: $TARGETS_COUNT (expected: >= 1)${NC}"
    exit 1
fi
echo ""

# Display schedule information
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}EventBridge Scheduling Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Schedule Details:${NC}"
echo -e "  Rule Name: $RULE_NAME"
echo -e "  Schedule: Daily at 9:00 AM UTC"
echo -e "  Status: ENABLED"
echo -e "  Target: Lambda function '$FUNCTION_NAME'"
echo ""
echo -e "${YELLOW}Next Execution:${NC}"
CURRENT_TIME=$(date -u +"%Y-%m-%d %H:%M:%S UTC")
echo -e "  Current Time: $CURRENT_TIME"
echo -e "  Next Run: Tomorrow at 09:00:00 UTC (if current time is past 9:00 AM)"
echo -e "  Next Run: Today at 09:00:00 UTC (if current time is before 9:00 AM)"
echo ""
echo -e "${YELLOW}Monitoring:${NC}"
echo -e "  CloudWatch Logs: $LOG_GROUP"
echo -e "  Log Retention: $LOG_RETENTION_DAYS days"
echo ""
echo -e "${YELLOW}Verification Commands:${NC}"
echo -e "  # List all scheduled rules"
echo -e "  aws events list-rules --region $AWS_REGION"
echo ""
echo -e "  # Check rule status"
echo -e "  aws events describe-rule --name $RULE_NAME --region $AWS_REGION"
echo ""
echo -e "  # View recent Lambda executions"
echo -e "  aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
echo -e "${YELLOW}AWS Console Links:${NC}"
echo -e "  EventBridge Rule:"
echo -e "  ${GREEN}https://console.aws.amazon.com/events/home?region=${AWS_REGION}#/eventbus/default/rules/${RULE_NAME}${NC}"
echo ""
echo -e "  Lambda Function:"
echo -e "  ${GREEN}https://console.aws.amazon.com/lambda/home?region=${AWS_REGION}#/functions/${FUNCTION_NAME}${NC}"
echo ""
echo -e "  CloudWatch Logs:"
echo -e "  ${GREEN}https://console.aws.amazon.com/cloudwatch/home?region=${AWS_REGION}#logsV2:log-groups/log-group/\$252Faws\$252Flambda\$252F${FUNCTION_NAME}${NC}"
echo ""
echo -e "${YELLOW}To disable scheduled execution:${NC}"
echo -e "  aws events disable-rule --name $RULE_NAME --region $AWS_REGION"
echo ""
echo -e "${YELLOW}To re-enable scheduled execution:${NC}"
echo -e "  aws events enable-rule --name $RULE_NAME --region $AWS_REGION"
echo ""
echo -e "${GREEN}Phase 6f complete! Lambda will now run automatically every day at 9:00 AM UTC.${NC}"
echo ""
