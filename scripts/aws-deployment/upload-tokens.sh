#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: Upload Gmail Tokens to Secrets Manager${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Configuration
AWS_REGION="${AWS_REGION:-us-east-1}"
SECRET_NAME="daily-task-orchestrator/gmail-tokens"
TOKEN_FILE="tokens/StoredCredential"

echo -e "${YELLOW}[1/3] Validating token file...${NC}"
if [ ! -f "$TOKEN_FILE" ]; then
    echo -e "${RED}Error: Token file not found at $TOKEN_FILE${NC}"
    echo -e "${YELLOW}Run: ./scripts/aws-deployment/init-gmail-tokens.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Token file found${NC}"
echo ""

echo -e "${YELLOW}[2/3] Reading token data...${NC}"
# Read the token file content
TOKEN_DATA=$(cat "$TOKEN_FILE")
echo -e "${GREEN}✓ Token data read (${#TOKEN_DATA} bytes)${NC}"
echo ""

echo -e "${YELLOW}[3/3] Uploading to AWS Secrets Manager...${NC}"

# Create JSON structure for Secrets Manager
# The SecretsManagerDataStore expects the token in a specific format
SECRET_JSON=$(cat <<EOF
{
  "StoredCredential": "$TOKEN_DATA"
}
EOF
)

# Upload to Secrets Manager
aws secretsmanager put-secret-value \
    --secret-id "$SECRET_NAME" \
    --secret-string "$SECRET_JSON" \
    --region "$AWS_REGION"

echo -e "${GREEN}✓ Tokens uploaded successfully${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Token Upload Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Secret: ${GREEN}${SECRET_NAME}${NC}"
echo -e "Region: ${GREEN}${AWS_REGION}${NC}"
echo ""
echo -e "${YELLOW}Next step: Run deploy-lambda.sh to deploy the Lambda function${NC}"
echo ""
