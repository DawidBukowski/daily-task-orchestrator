# Gmail OAuth2 Token Workflow

## Overview

The Daily Task Orchestrator uses Gmail OAuth2 to read emails. Tokens are initialized locally (interactive browser flow) and then uploaded to AWS Secrets Manager for Lambda access.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TOKEN LIFECYCLE                                │
└─────────────────────────────────────────────────────────────────────┘

  LOCAL MACHINE                          AWS CLOUD
  ─────────────                          ─────────

  ┌──────────────────┐                   ┌─────────────────────────┐
  │ init-gmail-      │                   │  AWS Secrets Manager    │
  │ tokens.sh        │                   │                         │
  │                  │   upload-         │  Secret:                │
  │  Browser OAuth   │   tokens.sh      │  daily-task-orchestrator│
  │  ↓               │  ──────────────► │  /gmail-tokens/         │
  │  Google grants   │                   │  StoredCredential       │
  │  ↓               │                   │                         │
  │  tokens/         │                   │  {                      │
  │  StoredCredential│                   │    "user": {            │
  │                  │                   │      "accessToken":..., │
  └──────────────────┘                   │      "refreshToken":...,│
                                         │      "expirationTime..."│
                                         │    }                    │
                                         │  }                      │
                                         └────────────┬────────────┘
                                                      │
                                                      │ loadCredential("user")
                                                      ▼
                                         ┌─────────────────────────┐
                                         │  Lambda Function        │
                                         │                         │
                                         │  GmailOAuth2Handler     │
                                         │  ↓                      │
                                         │  SecretsManagerDataStore│
                                         │  ↓                      │
                                         │  Token expired?         │
                                         │  YES → refreshToken()   │
                                         │       → save back to SM │
                                         │  NO  → use as-is       │
                                         └─────────────────────────┘
```

---

## Initial Token Setup

### Prerequisites
- Google Cloud project with Gmail API enabled
- OAuth client credentials (Desktop app type)
- Java 21, Maven, built JAR

### Steps

1. **Set environment variables:**
   ```bash
   export GMAIL_CLIENT_ID="your-client-id.apps.googleusercontent.com"
   export GMAIL_CLIENT_SECRET="your-client-secret"
   ```

2. **Build the project:**
   ```bash
   mvn clean package
   ```

3. **Run the initialization script:**
   ```bash
   ./scripts/aws-deployment/init-gmail-tokens.sh
   ```
   This runs the app locally with `DEPLOYMENT_ENV=local`, which triggers `FileDataStoreFactory` mode. A browser window opens for Google OAuth consent.

4. **Verify token file created:**
   ```bash
   ls -la tokens/StoredCredential
   ```

5. **Upload tokens to Secrets Manager:**
   ```bash
   ./scripts/aws-deployment/upload-tokens.sh
   ```
   This writes the token to secret `daily-task-orchestrator/gmail-tokens` in your configured region.

---

## Token Storage Format

### In AWS Secrets Manager

**Secret name:** `daily-task-orchestrator/gmail-tokens/StoredCredential`

**JSON structure:**
```json
{
  "user": {
    "accessToken": "ya29.a0AfH6SM...",
    "refreshToken": "1//0dx...",
    "expirationTimeMillis": 1720000000000
  }
}
```

The key `"user"` matches the credential ID used in `flow.loadCredential("user")`.

---

## Access Token Refresh Mechanism

The refresh is handled in `GmailOAuth2Handler.authenticate()` (Lambda mode):

1. Load credential from Secrets Manager via `SecretsManagerDataStore`
2. Check `credential.getExpiresInSeconds() < 60`
3. If expired → call `credential.refreshToken()`
4. Google OAuth server issues a new access token using the refresh token
5. `SecretsManagerDataStore.set()` persists the updated token back to Secrets Manager

The access token typically expires after **1 hour**. The Lambda runs once daily, so the token is always expired on invocation and gets refreshed automatically.

---

## Refresh Token Expiration

Google refresh tokens **do NOT expire** under normal conditions. They become invalid only when:

| Scenario | Effect | Remediation |
|----------|--------|-------------|
| User revokes app access in Google Account settings | Refresh token invalidated | Re-run `init-gmail-tokens.sh` + `upload-tokens.sh` |
| Token unused for 6 months | Token may expire | Daily Lambda execution prevents this |
| User changes Google password | All refresh tokens revoked | Re-run initialization |
| User exceeds 50 live refresh tokens | Oldest token revoked | Re-run initialization |
| App in "Testing" status in Google Cloud Console | Token expires after 7 days | Publish the app or re-initialize weekly |

**Important:** If your Google Cloud app is in "Testing" status (not published), tokens expire after 7 days. To avoid this, either publish the app or move it to "In production" status in the OAuth consent screen settings.

---

## Token Renewal Procedure

When Lambda fails with a Gmail authentication error:

1. **Check CloudWatch logs** for the error message:
   ```
   ERROR - No OAuth tokens found in Secrets Manager
   ```
   or
   ```
   ERROR - Authentication failed due to network or IO error
   ```

2. **Re-initialize tokens locally:**
   ```bash
   export GMAIL_CLIENT_ID="your-client-id"
   export GMAIL_CLIENT_SECRET="your-client-secret"
   ./scripts/aws-deployment/init-gmail-tokens.sh
   ```

3. **Upload new tokens:**
   ```bash
   ./scripts/aws-deployment/upload-tokens.sh
   ```

4. **Test Lambda manually:**
   ```bash
   ./scripts/aws-deployment/test-lambda.sh
   ```

---

## Failure Scenarios

### 1. Access token expired (normal operation)
- **Symptom:** None visible — handled automatically
- **What happens:** `GmailOAuth2Handler` detects `expiresInSeconds < 60`, calls `refreshToken()`, saves new token to Secrets Manager
- **Action needed:** None

### 2. Refresh token revoked
- **Symptom:** Lambda fails, CloudWatch logs show authentication error
- **What happens:** `credential.refreshToken()` fails, IOException thrown
- **Action needed:** Re-run `init-gmail-tokens.sh` + `upload-tokens.sh`

### 3. Secret deleted from Secrets Manager
- **Symptom:** Lambda fails immediately at startup
- **Log message:** `"No OAuth tokens found in Secrets Manager"`
- **Action needed:** Re-run `upload-tokens.sh` (if local `tokens/StoredCredential` still exists) or full re-initialization

### 4. Secrets Manager access denied
- **Symptom:** Lambda fails with AWS SDK exception
- **What happens:** IAM role missing `secretsmanager:GetSecretValue` permission
- **Action needed:** Re-run `scripts/aws-deployment/setup-iam.sh` to fix permissions

---

## Related Files

| File | Role |
|------|------|
| `src/main/java/.../gmail/GmailOAuth2Handler.java` | OAuth flow orchestration (Lambda vs local mode) |
| `src/main/java/.../gmail/SecretsManagerDataStore.java` | Token persistence in Secrets Manager |
| `src/main/java/.../gmail/SecretsManagerDataStoreFactory.java` | Factory for creating DataStore instances |
| `scripts/aws-deployment/init-gmail-tokens.sh` | Local OAuth initialization |
| `scripts/aws-deployment/upload-tokens.sh` | Token upload to Secrets Manager |
