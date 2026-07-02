# Daily Task Orchestrator

A Java-based application for orchestrating and managing daily tasks efficiently.

## Overview

Daily Task Orchestrator is a task management system designed to help you organize, schedule, and execute daily tasks with ease. Built with Java, it provides a robust and scalable solution for task automation and coordination.

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
