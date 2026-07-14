#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  Daily Task Orchestrator - Complete AWS Deployment  ${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${YELLOW}This script will execute all Phase 6e deployment steps:${NC}"
echo -e "  1. Setup IAM role and policies"
echo -e "  2. Create AWS Secrets Manager secrets"
echo -e "  3. Initialize Gmail OAuth tokens locally"
echo -e "  4. Upload tokens to Secrets Manager"
echo -e "  5. Deploy Lambda function"
echo -e "  6. Test Lambda function"
echo ""

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}Error: Must run from project root directory${NC}"
    exit 1
fi

# Check required environment variables
echo -e "${YELLOW}Checking required environment variables...${NC}"
REQUIRED_VARS=(
    "GMAIL_CLIENT_ID"
    "GMAIL_CLIENT_SECRET"
    "EMAIL_SMTP_HOST"
    "EMAIL_SMTP_PORT"
    "EMAIL_USERNAME"
    "EMAIL_PASSWORD"
    "EMAIL_FROM"
    "EMAIL_TO"
)

MISSING_VARS=()
for VAR in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!VAR}" ]; then
        MISSING_VARS+=("$VAR")
    fi
done

if [ ${#MISSING_VARS[@]} -ne 0 ]; then
    echo -e "${RED}Error: Missing required environment variables:${NC}"
    for VAR in "${MISSING_VARS[@]}"; do
        echo -e "${RED}  - $VAR${NC}"
    done
    exit 1
fi
echo -e "${GREEN}✓ All required environment variables are set${NC}"
echo ""

# Build the project first
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 0: Building project${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
mvn clean package -DskipTests
echo ""

# Step 1: IAM Setup
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 1: IAM Role Setup${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
bash "$SCRIPT_DIR/setup-iam.sh"
echo ""

# Step 2: Secrets Manager Setup
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 2: Secrets Manager Setup${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
bash "$SCRIPT_DIR/setup-secrets.sh"
echo ""

# Step 3: Initialize Gmail Tokens
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 3: Gmail Token Initialization${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
echo -e "${YELLOW}⚠ IMPORTANT: A browser window will open${NC}"
echo -e "${YELLOW}⚠ Please complete the Gmail OAuth flow${NC}"
echo ""
read -p "Press Enter to continue..."
echo ""
bash "$SCRIPT_DIR/init-gmail-tokens.sh"
echo ""

# Step 4: Upload Tokens
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 4: Upload Tokens to Secrets Manager${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
bash "$SCRIPT_DIR/upload-tokens.sh"
echo ""

# Step 5: Deploy Lambda
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 5: Lambda Function Deployment${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
bash "$SCRIPT_DIR/deploy-lambda.sh"
echo ""

# Step 6: Test Lambda
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}Step 6: Lambda Function Testing${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
bash "$SCRIPT_DIR/test-lambda.sh"
echo ""

# Summary
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}        Deployment Complete! 🎉${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""
echo -e "${GREEN}✓ IAM role created${NC}"
echo -e "${GREEN}✓ Secrets configured${NC}"
echo -e "${GREEN}✓ Gmail tokens initialized${NC}"
echo -e "${GREEN}✓ Lambda function deployed${NC}"
echo -e "${GREEN}✓ Lambda function tested${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Check your email for the task summary"
echo -e "  2. Review CloudWatch logs for any issues"
echo -e "  3. Run setup-eventbridge.sh to configure scheduled execution"
echo ""
echo -e "${YELLOW}Resources Created:${NC}"
echo -e "  - IAM Role: daily-task-orchestrator-lambda-role"
echo -e "  - IAM Policy: daily-task-orchestrator-lambda-policy"
echo -e "  - Secret: daily-task-orchestrator/app-config"
echo -e "  - Secret: daily-task-orchestrator/gmail-tokens"
echo -e "  - Lambda: daily-task-orchestrator"
echo ""
