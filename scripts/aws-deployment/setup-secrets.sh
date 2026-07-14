#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Phase 6e: Secrets Manager Setup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check required environment variables
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

echo -e "${YELLOW}[1/3] Validating environment variables...${NC}"
MISSING_VARS=()
for VAR in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!VAR}" ]; then
        MISSING_VARS+=("$VAR")
        echo -e "${RED}✗ Missing: $VAR${NC}"
    else
        echo -e "${GREEN}✓ Found: $VAR${NC}"
    fi
done

if [ ${#MISSING_VARS[@]} -ne 0 ]; then
    echo -e "${RED}Error: Missing required environment variables${NC}"
    echo -e "${RED}Please set: ${MISSING_VARS[*]}${NC}"
    exit 1
fi
echo ""

# Detect Claude provider (default to AWS_BEDROCK for Lambda)
CLAUDE_PROVIDER="${CLAUDE_PROVIDER:-AWS_BEDROCK}"
if [ "$CLAUDE_PROVIDER" = "AWS_BEDROCK" ]; then
    CLAUDE_MODEL_ID="${CLAUDE_MODEL_ID:-anthropic.claude-3-5-sonnet-20241022-v2:0}"
elif [ "$CLAUDE_PROVIDER" = "ANTHROPIC" ]; then
    CLAUDE_MODEL_ID="${CLAUDE_MODEL_ID:-claude-3-5-sonnet-20241022}"
    if [ -z "$ANTHROPIC_API_KEY" ]; then
        echo -e "${RED}Error: ANTHROPIC_API_KEY required when CLAUDE_PROVIDER=ANTHROPIC${NC}"
        exit 1
    fi
fi

# Get AWS region
AWS_REGION="${AWS_REGION:-us-east-1}"

echo -e "${YELLOW}[2/3] Creating app-config secret...${NC}"
SECRET_NAME="daily-task-orchestrator/app-config"

# Build JSON based on provider
if [ "$CLAUDE_PROVIDER" = "AWS_BEDROCK" ]; then
    SECRET_JSON=$(cat <<EOF
{
  "CLAUDE_PROVIDER": "$CLAUDE_PROVIDER",
  "CLAUDE_MODEL_ID": "$CLAUDE_MODEL_ID",
  "CLAUDE_MAX_TOKENS": "${CLAUDE_MAX_TOKENS:-1000}",
  "CLAUDE_TEMPERATURE": "${CLAUDE_TEMPERATURE:-0.3}",
  "CLAUDE_TIMEOUT_SECONDS": "${CLAUDE_TIMEOUT_SECONDS:-30}",
  "AWS_REGION": "$AWS_REGION",
  "GMAIL_CLIENT_ID": "$GMAIL_CLIENT_ID",
  "GMAIL_CLIENT_SECRET": "$GMAIL_CLIENT_SECRET",
  "EMAIL_SMTP_HOST": "$EMAIL_SMTP_HOST",
  "EMAIL_SMTP_PORT": "$EMAIL_SMTP_PORT",
  "EMAIL_USERNAME": "$EMAIL_USERNAME",
  "EMAIL_PASSWORD": "$EMAIL_PASSWORD",
  "EMAIL_FROM": "$EMAIL_FROM",
  "EMAIL_TO": "$EMAIL_TO",
  "EMAIL_ENABLE_TLS": "${EMAIL_ENABLE_TLS:-true}",
  "EMAIL_ENABLE_AUTH": "${EMAIL_ENABLE_AUTH:-true}",
  "EMAIL_TIMEOUT_MS": "${EMAIL_TIMEOUT_MS:-30000}"
}
EOF
)
else
    SECRET_JSON=$(cat <<EOF
{
  "CLAUDE_PROVIDER": "$CLAUDE_PROVIDER",
  "CLAUDE_MODEL_ID": "$CLAUDE_MODEL_ID",
  "CLAUDE_MAX_TOKENS": "${CLAUDE_MAX_TOKENS:-1000}",
  "CLAUDE_TEMPERATURE": "${CLAUDE_TEMPERATURE:-0.3}",
  "CLAUDE_TIMEOUT_SECONDS": "${CLAUDE_TIMEOUT_SECONDS:-30}",
  "ANTHROPIC_API_KEY": "$ANTHROPIC_API_KEY",
  "ANTHROPIC_API_URL": "${ANTHROPIC_API_URL:-https://api.anthropic.com/v1/messages}",
  "GMAIL_CLIENT_ID": "$GMAIL_CLIENT_ID",
  "GMAIL_CLIENT_SECRET": "$GMAIL_CLIENT_SECRET",
  "EMAIL_SMTP_HOST": "$EMAIL_SMTP_HOST",
  "EMAIL_SMTP_PORT": "$EMAIL_SMTP_PORT",
  "EMAIL_USERNAME": "$EMAIL_USERNAME",
  "EMAIL_PASSWORD": "$EMAIL_PASSWORD",
  "EMAIL_FROM": "$EMAIL_FROM",
  "EMAIL_TO": "$EMAIL_TO",
  "EMAIL_ENABLE_TLS": "${EMAIL_ENABLE_TLS:-true}",
  "EMAIL_ENABLE_AUTH": "${EMAIL_ENABLE_AUTH:-true}",
  "EMAIL_TIMEOUT_MS": "${EMAIL_TIMEOUT_MS:-30000}"
}
EOF
)
fi

# Create or update secret
if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" 2>/dev/null; then
    echo -e "${YELLOW}⚠ Secret already exists, updating...${NC}"
    aws secretsmanager put-secret-value \
        --secret-id "$SECRET_NAME" \
        --secret-string "$SECRET_JSON" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Secret updated${NC}"
else
    aws secretsmanager create-secret \
        --name "$SECRET_NAME" \
        --description "Application configuration for Daily Task Orchestrator Lambda" \
        --secret-string "$SECRET_JSON" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Secret created${NC}"
fi
echo ""

echo -e "${YELLOW}[3/3] Creating gmail-tokens secret (empty placeholder)...${NC}"
TOKENS_SECRET_NAME="daily-task-orchestrator/gmail-tokens"
EMPTY_TOKENS='{"installed": {}, "credentials": {}}'

if aws secretsmanager describe-secret --secret-id "$TOKENS_SECRET_NAME" --region "$AWS_REGION" 2>/dev/null; then
    echo -e "${YELLOW}⚠ Tokens secret already exists, skipping${NC}"
else
    aws secretsmanager create-secret \
        --name "$TOKENS_SECRET_NAME" \
        --description "Gmail OAuth tokens for Daily Task Orchestrator Lambda" \
        --secret-string "$EMPTY_TOKENS" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ Tokens secret created (empty placeholder)${NC}"
fi
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Secrets Manager Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Config secret: ${GREEN}${SECRET_NAME}${NC}"
echo -e "Tokens secret: ${GREEN}${TOKENS_SECRET_NAME}${NC}"
echo -e "Region: ${GREEN}${AWS_REGION}${NC}"
echo ""
echo -e "${YELLOW}Next step: Run init-gmail-tokens.sh to generate tokens${NC}"
echo ""
