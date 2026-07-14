#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: Lambda Function Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Configuration
FUNCTION_NAME="daily-task-orchestrator"
HANDLER="com.dailytask.lambda.DailyTaskLambdaHandler::handleRequest"
RUNTIME="java21"
MEMORY_SIZE=1024
TIMEOUT=300
JAR_PATH="target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar"
AWS_REGION="${AWS_REGION:-us-east-1}"
ROLE_NAME="daily-task-orchestrator-lambda-role"

echo -e "${YELLOW}[1/5] Validating prerequisites...${NC}"

# Check JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Error: JAR file not found at $JAR_PATH${NC}"
    echo -e "${YELLOW}Run: mvn clean package${NC}"
    exit 1
fi
echo -e "${GREEN}✓ JAR file found ($(du -h "$JAR_PATH" | cut -f1))${NC}"

# Get account ID and role ARN
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"

# Verify role exists
if ! aws iam get-role --role-name "$ROLE_NAME" 2>/dev/null >/dev/null; then
    echo -e "${RED}Error: IAM role not found: $ROLE_NAME${NC}"
    echo -e "${YELLOW}Run: ./scripts/aws-deployment/setup-iam.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✓ IAM role verified${NC}"
echo ""

echo -e "${YELLOW}[2/5] Packaging Lambda deployment...${NC}"
ZIP_FILE="/tmp/daily-task-orchestrator-lambda.zip"
cp "$JAR_PATH" "$ZIP_FILE"
echo -e "${GREEN}✓ Deployment package ready: $(du -h "$ZIP_FILE" | cut -f1)${NC}"
echo ""

echo -e "${YELLOW}[3/5] Checking if Lambda function exists...${NC}"
if aws lambda get-function --function-name "$FUNCTION_NAME" --region "$AWS_REGION" 2>/dev/null >/dev/null; then
    echo -e "${YELLOW}⚠ Function exists, updating code...${NC}"

    aws lambda update-function-code \
        --function-name "$FUNCTION_NAME" \
        --zip-file "fileb://$ZIP_FILE" \
        --region "$AWS_REGION" \
        --output json | jq -r '.FunctionArn'

    echo -e "${GREEN}✓ Function code updated${NC}"

    # Wait for update to complete
    echo -e "${YELLOW}Waiting for update to complete...${NC}"
    aws lambda wait function-updated \
        --function-name "$FUNCTION_NAME" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Update complete${NC}"

else
    echo -e "${YELLOW}Creating new Lambda function...${NC}"

    aws lambda create-function \
        --function-name "$FUNCTION_NAME" \
        --runtime "$RUNTIME" \
        --role "$ROLE_ARN" \
        --handler "$HANDLER" \
        --zip-file "fileb://$ZIP_FILE" \
        --timeout "$TIMEOUT" \
        --memory-size "$MEMORY_SIZE" \
        --region "$AWS_REGION" \
        --output json | jq -r '.FunctionArn'

    echo -e "${GREEN}✓ Function created${NC}"

    # Wait for function to be active
    echo -e "${YELLOW}Waiting for function to be active...${NC}"
    aws lambda wait function-active \
        --function-name "$FUNCTION_NAME" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Function active${NC}"
fi
echo ""

echo -e "${YELLOW}[4/5] Configuring environment variables...${NC}"
aws lambda update-function-configuration \
    --function-name "$FUNCTION_NAME" \
    --environment "Variables={DEPLOYMENT_ENV=lambda,AWS_REGION=$AWS_REGION}" \
    --region "$AWS_REGION" \
    --output json | jq -r '.FunctionArn'

echo -e "${GREEN}✓ Environment variables configured${NC}"

# Wait for configuration update
echo -e "${YELLOW}Waiting for configuration update...${NC}"
aws lambda wait function-updated \
    --function-name "$FUNCTION_NAME" \
    --region "$AWS_REGION"
echo -e "${GREEN}✓ Configuration updated${NC}"
echo ""

echo -e "${YELLOW}[5/5] Retrieving function details...${NC}"
FUNCTION_ARN=$(aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --region "$AWS_REGION" \
    --query 'Configuration.FunctionArn' \
    --output text)

CODE_SIZE=$(aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --region "$AWS_REGION" \
    --query 'Configuration.CodeSize' \
    --output text)

LAST_MODIFIED=$(aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --region "$AWS_REGION" \
    --query 'Configuration.LastModified' \
    --output text)

echo -e "${GREEN}✓ Function details retrieved${NC}"
echo ""

# Cleanup
rm -f "$ZIP_FILE"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Lambda Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Function Name: ${GREEN}${FUNCTION_NAME}${NC}"
echo -e "Function ARN: ${GREEN}${FUNCTION_ARN}${NC}"
echo -e "Runtime: ${GREEN}${RUNTIME}${NC}"
echo -e "Handler: ${GREEN}${HANDLER}${NC}"
echo -e "Memory: ${GREEN}${MEMORY_SIZE} MB${NC}"
echo -e "Timeout: ${GREEN}${TIMEOUT} seconds${NC}"
echo -e "Code Size: ${GREEN}$(numfmt --to=iec-i --suffix=B $CODE_SIZE)${NC}"
echo -e "Last Modified: ${GREEN}${LAST_MODIFIED}${NC}"
echo -e "Region: ${GREEN}${AWS_REGION}${NC}"
echo ""
echo -e "${YELLOW}Next step: Run test-lambda.sh to test the function${NC}"
echo ""
