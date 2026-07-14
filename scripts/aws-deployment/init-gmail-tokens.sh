#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: Gmail Token Initialization${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if JAR exists
JAR_PATH="target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Error: JAR file not found at $JAR_PATH${NC}"
    echo -e "${YELLOW}Run: mvn clean package${NC}"
    exit 1
fi

# Check required environment variables
if [ -z "$GMAIL_CLIENT_ID" ] || [ -z "$GMAIL_CLIENT_SECRET" ]; then
    echo -e "${RED}Error: GMAIL_CLIENT_ID and GMAIL_CLIENT_SECRET must be set${NC}"
    exit 1
fi

echo -e "${YELLOW}[1/3] Setting up local environment...${NC}"
export DEPLOYMENT_ENV=local
export CLAUDE_PROVIDER="${CLAUDE_PROVIDER:-AWS_BEDROCK}"
export CLAUDE_MODEL_ID="${CLAUDE_MODEL_ID:-anthropic.claude-3-5-sonnet-20241022-v2:0}"
export AWS_REGION="${AWS_REGION:-us-east-1}"

# Set dummy email config (not needed for token initialization)
export EMAIL_SMTP_HOST="${EMAIL_SMTP_HOST:-smtp.gmail.com}"
export EMAIL_SMTP_PORT="${EMAIL_SMTP_PORT:-587}"
export EMAIL_USERNAME="${EMAIL_USERNAME:-dummy@gmail.com}"
export EMAIL_PASSWORD="${EMAIL_PASSWORD:-dummy}"
export EMAIL_FROM="${EMAIL_FROM:-dummy@gmail.com}"
export EMAIL_TO="${EMAIL_TO:-dummy@gmail.com}"

echo -e "${GREEN}✓ Environment configured for local execution${NC}"
echo ""

echo -e "${YELLOW}[2/3] Running application to initialize Gmail OAuth flow...${NC}"
echo -e "${YELLOW}⚠ A browser window will open for Gmail authentication${NC}"
echo -e "${YELLOW}⚠ Please login and grant permissions${NC}"
echo ""

# Create tokens directory if it doesn't exist
mkdir -p tokens

# Run the application
java -jar "$JAR_PATH"

echo ""
echo -e "${YELLOW}[3/3] Verifying tokens were created...${NC}"

# Check for token file (SecretsManagerDataStore creates it)
TOKEN_FILE="tokens/StoredCredential"
if [ -f "$TOKEN_FILE" ]; then
    echo -e "${GREEN}✓ Tokens generated successfully${NC}"
    echo -e "${GREEN}✓ Token file: $TOKEN_FILE${NC}"
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Gmail Token Initialization Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${YELLOW}Next step: Run upload-tokens.sh to upload to AWS Secrets Manager${NC}"
    echo ""
else
    echo -e "${RED}✗ Token file not found${NC}"
    echo -e "${RED}OAuth flow may have failed${NC}"
    exit 1
fi
