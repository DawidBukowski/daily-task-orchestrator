#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: IAM Role Setup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Configuration
ROLE_NAME="daily-task-orchestrator-lambda-role"
POLICY_NAME="daily-task-orchestrator-lambda-policy"

echo -e "${YELLOW}[1/4] Creating IAM role trust policy...${NC}"
cat > /tmp/trust-policy.json << 'EOF'
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
EOF

echo -e "${GREEN}✓ Trust policy created${NC}"
echo ""

echo -e "${YELLOW}[2/4] Creating IAM role: ${ROLE_NAME}${NC}"
if aws iam get-role --role-name "$ROLE_NAME" 2>/dev/null; then
    echo -e "${YELLOW}⚠ Role already exists, skipping creation${NC}"
else
    aws iam create-role \
        --role-name "$ROLE_NAME" \
        --assume-role-policy-document file:///tmp/trust-policy.json \
        --description "Execution role for Daily Task Orchestrator Lambda function"
    echo -e "${GREEN}✓ Role created successfully${NC}"
fi
echo ""

echo -e "${YELLOW}[3/4] Creating IAM policy with permissions...${NC}"
cat > /tmp/lambda-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/aws/lambda/daily-task-orchestrator*"
    },
    {
      "Sid": "SecretsManager",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:PutSecretValue",
        "secretsmanager:UpdateSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:daily-task-orchestrator/*"
      ]
    },
    {
      "Sid": "BedrockInvoke",
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel"
      ],
      "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude*"
    }
  ]
}
EOF

# Check if policy exists
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

if aws iam get-policy --policy-arn "$POLICY_ARN" 2>/dev/null; then
    echo -e "${YELLOW}⚠ Policy already exists, updating...${NC}"
    # Create new version
    aws iam create-policy-version \
        --policy-arn "$POLICY_ARN" \
        --policy-document file:///tmp/lambda-policy.json \
        --set-as-default
    echo -e "${GREEN}✓ Policy updated${NC}"
else
    aws iam create-policy \
        --policy-name "$POLICY_NAME" \
        --policy-document file:///tmp/lambda-policy.json \
        --description "Permissions for Daily Task Orchestrator Lambda"
    echo -e "${GREEN}✓ Policy created${NC}"
fi
echo ""

echo -e "${YELLOW}[4/4] Attaching policy to role...${NC}"
aws iam attach-role-policy \
    --role-name "$ROLE_NAME" \
    --policy-arn "$POLICY_ARN" 2>/dev/null || echo -e "${YELLOW}⚠ Policy already attached${NC}"
echo -e "${GREEN}✓ Policy attached${NC}"
echo ""

# Wait for role to be available
echo -e "${YELLOW}Waiting for IAM role to propagate (10 seconds)...${NC}"
sleep 10
echo -e "${GREEN}✓ IAM role ready${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}IAM Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Role ARN: ${GREEN}arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}${NC}"
echo -e "Policy ARN: ${GREEN}${POLICY_ARN}${NC}"
echo ""

# Cleanup temp files
rm -f /tmp/trust-policy.json /tmp/lambda-policy.json
