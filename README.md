# Daily Task Orchestrator

**Status:** Deployed on AWS Lambda | Runs daily at 9:00 AM UTC via EventBridge

A Java-based application for orchestrating and managing daily tasks efficiently with AI-powered summarization.

## Overview

Daily Task Orchestrator is a task management system that automatically reads your Gmail inbox, analyzes tasks using Claude AI (via AWS Bedrock), and sends you a prioritized HTML email summary every morning. Built with Java 21 and deployed as a serverless AWS Lambda function.

## Features

- **Task Management**: Create, update, and manage daily tasks
- **Task Scheduling**: Schedule tasks for specific times and dates
- **Task Orchestration**: Coordinate multiple tasks efficiently
- **Progress Tracking**: Monitor task execution and completion status

## 📚 Documentation

### Claude Integration Documentation
- **[CLAUDE.md](CLAUDE.md)** - Complete guide for Claude integration setup, configuration, development guidelines, and agent dispatch protocol
- **[docs/03_Claude/ARCHITECTURE.md](docs/03_Claude/ARCHITECTURE.md)** - Deep dive into hexagonal architecture, component relationships, Claude provider flow, and testing strategies
- **[docs/03_Claude/TROUBLESHOOTING.md](docs/03_Claude/TROUBLESHOOTING.md)** - Common issues, solutions, error diagnosis, and quick reference

### AWS Deployment Documentation
- **[Deployment Guide](docs/06_AWS_Deployment/DEPLOYMENT_GUIDE.md)** - Complete AWS Lambda deployment instructions
- **[Deployment Scripts](scripts/aws-deployment/README.md)** - Automated deployment script documentation

### Project Implementation Documentation
- **[Phase 3 Implementation Summary](docs/00_Meta/PHASE3_IMPLEMENTATION_SUMMARY.md)** - Task domain model, normalization pipeline, deadline parsing (6 formats), title extraction
- **[Architecture Guide](docs/01_Architecture/Architecture.md)** - Step-by-step execution flow, hexagonal architecture, dependency injection

## Requirements

- Java 21
- Maven 3.6 or higher (if using Maven)

## Installation

### Clone the Repository

```bash
git clone https://github.com/Alucart558/daily-task-orchestrator.git
cd daily-task-orchestrator
```

### Build the Project

Using Maven:
```bash
mvn clean install
```

## Usage

### Running the Application

```bash
java -jar target/daily-task-orchestrator.jar
```

## Project Structure

```
daily-task-orchestrator/
├── src/
│   ├── main/
│   │   └── java/
│   └── test/
│       └── java/
├── pom.xml (Maven)
└── README.md
```

## Claude Setup

The application integrates with Claude AI for task summarization through **two provider modes**:

### Option 1: Direct Anthropic API (Recommended for Development)

**Quick Start:**
```bash
export CLAUDE_PROVIDER=ANTHROPIC
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
export ANTHROPIC_API_KEY=sk-ant-api03-...
```

**Key Features:**
- Simple setup, no additional infrastructure
- Direct API access with predictable latency
- Best for development and testing

**Get Your API Key:**
1. Visit [console.anthropic.com](https://console.anthropic.com/)
2. Navigate to API Keys
3. Create a new key and copy it

**Recommended Models:**
- `claude-3-5-sonnet-20241022` - Best balance of cost and capability
- `claude-3-opus-20240229` - Maximum intelligence
- `claude-3-haiku-20240307` - Fastest, most economical

### Option 2: AWS Bedrock (Recommended for Production)

**Quick Start:**
```bash
export CLAUDE_PROVIDER=AWS_BEDROCK
export CLAUDE_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
export AWS_REGION=us-east-1
# AWS credentials via environment variables or ~/.aws/credentials
```

**Key Features:**
- Enterprise-grade security with IAM roles
- Works with existing AWS infrastructure
- Audit logging via CloudTrail

**Setup Steps:**
1. Create IAM user with `bedrock:InvokeModel` permission
2. Set AWS region (us-east-1, us-west-2, eu-west-1, etc.)
3. Request model access in AWS Bedrock console
4. Configure AWS credentials

**AWS Credentials:**
```bash
# Option A: Environment variables
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret

# Option B: Credentials file (~/.aws/credentials)
# Option C: IAM role (when running on EC2/ECS/Lambda)
```

### Optional Configuration

Both providers support these environment variables:

```bash
# Response size (default: 1000)
export CLAUDE_MAX_TOKENS=2000

# Response randomness 0.0-1.0 (default: 0.3)
export CLAUDE_TEMPERATURE=0.5

# Request timeout in seconds (default: 30)
export CLAUDE_TIMEOUT_SECONDS=60
```

### Key Features

- **Graceful Fallback** - If Claude is unavailable, the application logs the error and continues
- **Timeout Handling** - Configurable timeouts prevent hanging requests
- **Secure** - No sensitive data logged, API keys in environment only
- **Flexible** - Switch providers by changing one environment variable

### Testing Your Configuration

```bash
# Build the project
mvn clean package

# Run with environment variables
export CLAUDE_PROVIDER=ANTHROPIC
export CLAUDE_MODEL_ID=claude-3-5-sonnet-20241022
export ANTHROPIC_API_KEY=your-key
java -jar target/daily-task-orchestrator.jar
```

Check logs for:
```
INFO  c.d.c.c.AppConfig - Initializing Claude API client with provider: ANTHROPIC
INFO  c.d.c.c.AppConfig - Using direct Anthropic API client
```

### Complete Claude Documentation

For detailed information including:
- All environment variables
- Error handling and troubleshooting
- Adding new providers
- Security best practices
- Cost optimization

See **[CLAUDE.md](CLAUDE.md)** in the project root.

---

## Gmail API Integration Setup

This project uses the official Google Gmail API to fetch raw emails.

### 1. Configure Credentials
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project or select an existing one.
3. Enable the **Gmail API** in "APIs & Services".
4. Go to **Credentials** -> Create Credentials -> **OAuth client ID**.
5. Application type: **Desktop app** (or Web application with redirect URI `http://localhost:8888/Callback`).
6. Copy your Client ID and Client Secret.

### 2. Environment Variables
Export the credentials before running the application:
```bash
export GMAIL_CLIENT_ID="your-client-id"
export GMAIL_CLIENT_SECRET="your-client-secret"
```

---

## Email Notification Setup

The Daily Task Orchestrator sends HTML-formatted task summaries to your email inbox via Gmail SMTP.

### Quick Start

**Required Environment Variables:**
```bash
export EMAIL_SMTP_HOST=smtp.gmail.com
export EMAIL_SMTP_PORT=587
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-gmail-app-password
export EMAIL_FROM=your-email@gmail.com
export EMAIL_TO=recipient@gmail.com
```

**Optional Settings (with defaults):**
```bash
export EMAIL_ENABLE_TLS=true      # Enable STARTTLS encryption
export EMAIL_ENABLE_AUTH=true     # Enable SMTP authentication
export EMAIL_TIMEOUT_MS=30000     # Connection timeout (30 seconds)
```

### Gmail App Password Setup

Gmail requires an **App Password** for SMTP access (not your regular Gmail password).

**Prerequisites:**
1. Enable 2-Factor Authentication on your Gmail account
2. Generate a Gmail App Password

**Step-by-Step Guide:**

1. **Enable 2-Factor Authentication:**
   - Visit [Google Account Security](https://myaccount.google.com/security)
   - Find "2-Step Verification" → Turn on
   - Follow phone verification steps

2. **Generate App Password:**
   - Visit [Google App Passwords](https://myaccount.google.com/apppasswords)
   - Select "Mail" and "Other (Custom name)"
   - Name: "Daily Task Orchestrator"
   - Click "Generate"
   - Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)
   - **Remove all spaces:** `abcdefghijklmnop`

3. **Set Environment Variable:**
   ```bash
   export EMAIL_PASSWORD=abcdefghijklmnop  # No spaces!
   ```

### Email Features

**What you'll receive:**
- **Subject:** "Daily Task Summary: X task(s)" or "⚠️ Daily Task Summary: X task(s) (Y overdue)"
- **Priority Color Coding:**
  - 🔴 CRITICAL - Red badge with white text
  - 🟠 HIGH - Orange badge with white text
  - 🟡 MEDIUM - Yellow badge with dark text
  - 🟢 LOW - Green badge with dark text
- **Overdue Warnings:** Red "⚠️ OVERDUE" text for past-due tasks
- **Gmail Links:** Click to open original email in Gmail
- **AI Recommendations:** Green-bordered section with actionable insights
- **Responsive Design:** Works on desktop, mobile, and tablet

### Testing Your Email Setup

```bash
# Build the project
mvn clean package

# Run with email notifications enabled
java -jar target/daily-task-orchestrator.jar
```

**Expected console output:**
```
INFO  c.d.core.config.AppConfig - Email notifier initialized: SMTP smtp.gmail.com:587, recipient: recipient@gmail.com
INFO  c.d.adapters.notifiers.EmailTaskNotifier - Generating email for 5 tasks
INFO  c.d.adapters.notifiers.SmtpEmailSender - Email sent to recipient@gmail.com
INFO  c.d.adapters.notifiers.EmailTaskNotifier - Email notification sent successfully
```

### Troubleshooting

**"Authentication failed (535 error)":**
- Verify App Password has no spaces: `abcdefghijklmnop`
- Ensure 2-Factor Authentication is enabled
- Regenerate App Password if unsure

**"Email not received":**
- Check spam/promotions folders
- Verify `EMAIL_TO` environment variable is set
- Check application logs for error messages

**"Timeout error":**
- Increase timeout: `export EMAIL_TIMEOUT_MS=60000`
- Check firewall rules (port 587 must be open)
- Try alternate port: `export EMAIL_SMTP_PORT=465` (SSL)

### Complete Email Documentation

For detailed setup instructions, troubleshooting, security best practices, and alternative SMTP providers, see:

**[docs/EMAIL_SETUP.md](docs/EMAIL_SETUP.md)** - Comprehensive email notification setup guide

---

## AWS Lambda Deployment

The Daily Task Orchestrator can be deployed to AWS Lambda for **automatic daily execution** at 9:00 AM via EventBridge scheduled events.

### Quick Deployment Overview

**Key Features:**
- ☁️ Fully serverless - runs on AWS Lambda
- 📅 Automatic scheduling via EventBridge (cron trigger)
- 🔐 Secure credential storage in AWS Secrets Manager
- 🤖 Claude AI integration via AWS Bedrock
- 📧 Email notifications after each run
- 💰 Estimated cost: ~$1.12/month (~$13/year)

### Prerequisites

- AWS Account with CLI configured
- Java 21 and Maven 3.6+
- Gmail OAuth tokens initialized locally
- AWS Bedrock model access (Anthropic Claude)

### Build Lambda Package

```bash
mvn clean package
```

**Output:** `target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar` (~30 MB fat JAR)

### Deployment Steps

1. **Create AWS Secrets Manager secrets** for app config and Gmail tokens
2. **Initialize Gmail OAuth tokens locally**, then upload to Secrets Manager
3. **Create IAM role** with permissions for Secrets Manager, Bedrock, CloudWatch
4. **Deploy Lambda function** with handler: `com.dailytask.lambda.DailyTaskLambdaHandler::handleRequest`
5. **Configure EventBridge rule** with cron expression: `cron(0 9 * * ? *)`
6. **Test deployment** with manual Lambda invocation

### Environment Variables (Lambda)

```bash
DEPLOYMENT_ENV=lambda     # Triggers AWS Secrets Manager mode
AWS_REGION=us-east-1      # Region for Secrets Manager and Bedrock
```

All other configuration (Gmail credentials, Claude settings, email config) is stored in **AWS Secrets Manager** under:
- `daily-task-orchestrator/app-config` - JSON with all env vars
- `daily-task-orchestrator/gmail-tokens` - OAuth tokens for Gmail API

### Testing Locally with SAM CLI

```bash
sam local invoke DailyTaskFunction --event events/scheduled-event.json
```

### Complete Deployment Guide

For step-by-step instructions on AWS infrastructure setup, IAM permissions, EventBridge configuration, monitoring, and troubleshooting, see:

**[docs/06_AWS_Deployment/DEPLOYMENT_GUIDE.md](docs/06_AWS_Deployment/DEPLOYMENT_GUIDE.md)** - Comprehensive AWS Lambda deployment guide

---
