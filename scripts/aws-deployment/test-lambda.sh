#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: Lambda Function Testing${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Configuration
FUNCTION_NAME="daily-task-orchestrator"
AWS_REGION="${AWS_REGION:-us-east-1}"
OUTPUT_FILE="/tmp/lambda-response.json"

echo -e "${YELLOW}[1/3] Creating test event payload...${NC}"

# Create EventBridge scheduled event
cat > /tmp/test-event.json << 'EOF'
{
  "id": "test-manual-invocation",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "account": "123456789012",
  "time": "2026-07-14T09:00:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:events:us-east-1:123456789012:rule/daily-task-orchestrator-9am"
  ],
  "detail": {}
}
EOF

echo -e "${GREEN}✓ Test event created${NC}"
echo ""

echo -e "${YELLOW}[2/3] Invoking Lambda function...${NC}"
echo -e "${YELLOW}This may take up to 5 minutes depending on email volume...${NC}"
echo ""

START_TIME=$(date +%s)

aws lambda invoke \
    --function-name "$FUNCTION_NAME" \
    --payload file:///tmp/test-event.json \
    --region "$AWS_REGION" \
    --log-type Tail \
    --query 'LogResult' \
    --output text \
    "$OUTPUT_FILE" | base64 --decode

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}✓ Invocation completed in ${DURATION} seconds${NC}"
echo ""

echo -e "${YELLOW}[3/3] Checking invocation result...${NC}"

if [ -f "$OUTPUT_FILE" ]; then
    RESPONSE=$(cat "$OUTPUT_FILE")
    echo -e "${YELLOW}Lambda Response:${NC}"
    echo "$RESPONSE"
    echo ""

    if echo "$RESPONSE" | grep -q '"SUCCESS"'; then
        echo -e "${GREEN}✓ Lambda execution: SUCCESS${NC}"
    elif echo "$RESPONSE" | grep -q 'errorMessage'; then
        echo -e "${RED}✗ Lambda execution: FAILED${NC}"
        echo -e "${RED}Error details:${NC}"
        echo "$RESPONSE" | jq -r '.errorMessage'
        exit 1
    else
        echo -e "${YELLOW}⚠ Unexpected response format${NC}"
    fi
else
    echo -e "${RED}✗ No response file generated${NC}"
    exit 1
fi
echo ""

echo -e "${YELLOW}Fetching recent CloudWatch logs...${NC}"
LOG_GROUP="/aws/lambda/$FUNCTION_NAME"

# Get the latest log stream
LATEST_STREAM=$(aws logs describe-log-streams \
    --log-group-name "$LOG_GROUP" \
    --order-by LastEventTime \
    --descending \
    --max-items 1 \
    --region "$AWS_REGION" \
    --query 'logStreams[0].logStreamName' \
    --output text 2>/dev/null || echo "")

if [ -n "$LATEST_STREAM" ] && [ "$LATEST_STREAM" != "None" ]; then
    echo -e "${YELLOW}Latest log stream: $LATEST_STREAM${NC}"
    echo ""
    echo -e "${YELLOW}CloudWatch Logs (last 50 lines):${NC}"
    echo -e "${YELLOW}----------------------------------------${NC}"

    aws logs get-log-events \
        --log-group-name "$LOG_GROUP" \
        --log-stream-name "$LATEST_STREAM" \
        --region "$AWS_REGION" \
        --limit 50 \
        --query 'events[*].message' \
        --output text

    echo -e "${YELLOW}----------------------------------------${NC}"
    echo ""
else
    echo -e "${YELLOW}⚠ No CloudWatch logs available yet${NC}"
    echo ""
fi

# Cleanup
rm -f /tmp/test-event.json "$OUTPUT_FILE"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Lambda Testing Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Check your email (${EMAIL_TO:-configured address}) for the task summary${NC}"
echo ""
echo -e "View logs in AWS Console:"
echo -e "${GREEN}https://console.aws.amazon.com/cloudwatch/home?region=${AWS_REGION}#logsV2:log-groups/log-group/\$252Faws\$252Flambda\$252F${FUNCTION_NAME}${NC}"
echo ""
echo -e "${YELLOW}Next step: Run setup-eventbridge.sh to configure scheduled execution${NC}"
echo ""
