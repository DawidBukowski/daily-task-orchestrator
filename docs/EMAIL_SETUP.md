# Email Notification Setup Guide

## Overview

This guide walks you through configuring the Daily Task Orchestrator to send HTML-formatted task summaries to your email inbox via Gmail SMTP.

**What you'll accomplish:**
- Enable 2-Factor Authentication on your Gmail account
- Generate a Gmail App Password for secure SMTP access
- Configure environment variables for email notifications
- Test the email notification system

**Time required:** 10-15 minutes

---

## Prerequisites

- A Gmail account (personal or Google Workspace)
- Administrative access to your Gmail security settings
- Terminal/command prompt access to set environment variables

---

## Step 1: Enable 2-Factor Authentication

Gmail App Passwords require 2-Factor Authentication (2FA) to be enabled on your account.

### Check if 2FA is Already Enabled

1. Visit [Google Account Security](https://myaccount.google.com/security)
2. Scroll to "Signing in to Google" section
3. Look for "2-Step Verification" status

**If already enabled:** Proceed to Step 2  
**If not enabled:** Follow the instructions below

### Enable 2-Factor Authentication

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Under "Signing in to Google", click **2-Step Verification**
3. Click **Get Started**
4. Sign in with your Gmail password
5. Select your verification method:
   - **Phone SMS** (recommended for most users)
   - **Authenticator app** (Google Authenticator, Authy, etc.)
   - **Security key** (hardware token)
6. Follow the on-screen prompts to complete setup
7. Verify 2FA is working by signing out and signing back in

**Verification:** After setup, you should see "2-Step Verification: On" in your security settings.

---

## Step 2: Generate Gmail App Password

App Passwords are 16-character passcodes that allow applications to access your Gmail account without exposing your main password.

### Create App Password

1. Visit [Google App Passwords](https://myaccount.google.com/apppasswords)
   - **Note:** If you see "2-Step Verification is not set up", complete Step 1 first
   - **Google Workspace users:** Your admin may need to enable App Passwords

2. Select app and device:
   - **Select app:** Choose "Mail"
   - **Select device:** Choose "Other (Custom name)"
   - **Name:** Enter `Daily Task Orchestrator`

3. Click **Generate**

4. Google displays a 16-character password like:
   ```
   abcd efgh ijkl mnop
   ```

5. **IMPORTANT:** Copy this password immediately - it won't be shown again
   - Remove all spaces: `abcdefghijklmnop`
   - Store securely (password manager recommended)

**Troubleshooting:**
- **"App Passwords not available":** Ensure 2FA is enabled and try again
- **"Less secure app access":** This is different - use App Passwords instead
- **Google Workspace:** Contact your admin to enable App Passwords

---

## Step 3: Configure Environment Variables

Set the following environment variables with your Gmail SMTP credentials.

### Required Variables

| Variable | Value | Example |
|----------|-------|---------|
| `EMAIL_SMTP_HOST` | Gmail SMTP server | `smtp.gmail.com` |
| `EMAIL_SMTP_PORT` | SMTP port | `587` (STARTTLS) |
| `EMAIL_USERNAME` | Your Gmail address | `your-email@gmail.com` |
| `EMAIL_PASSWORD` | App Password (no spaces) | `abcdefghijklmnop` |
| `EMAIL_FROM` | Sender address (same as username) | `your-email@gmail.com` |
| `EMAIL_TO` | Recipient email | `recipient@gmail.com` |

### Optional Variables (with defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_ENABLE_TLS` | `true` | Enable STARTTLS encryption |
| `EMAIL_ENABLE_AUTH` | `true` | Enable SMTP authentication |
| `EMAIL_TIMEOUT_MS` | `30000` | Connection timeout (30 seconds) |

### Setting Variables on Windows (PowerShell)

```powershell
# Persistent (survives terminal restart)
[System.Environment]::SetEnvironmentVariable('EMAIL_SMTP_HOST', 'smtp.gmail.com', 'User')
[System.Environment]::SetEnvironmentVariable('EMAIL_SMTP_PORT', '587', 'User')
[System.Environment]::SetEnvironmentVariable('EMAIL_USERNAME', 'your-email@gmail.com', 'User')
[System.Environment]::SetEnvironmentVariable('EMAIL_PASSWORD', 'abcdefghijklmnop', 'User')
[System.Environment]::SetEnvironmentVariable('EMAIL_FROM', 'your-email@gmail.com', 'User')
[System.Environment]::SetEnvironmentVariable('EMAIL_TO', 'recipient@gmail.com', 'User')

# Restart terminal to apply changes
```

**Session-only (temporary):**
```powershell
$env:EMAIL_SMTP_HOST = "smtp.gmail.com"
$env:EMAIL_SMTP_PORT = "587"
$env:EMAIL_USERNAME = "your-email@gmail.com"
$env:EMAIL_PASSWORD = "abcdefghijklmnop"
$env:EMAIL_FROM = "your-email@gmail.com"
$env:EMAIL_TO = "recipient@gmail.com"
```

### Setting Variables on Linux/macOS (Bash)

```bash
# Add to ~/.bashrc or ~/.zshrc for persistence
export EMAIL_SMTP_HOST=smtp.gmail.com
export EMAIL_SMTP_PORT=587
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=abcdefghijklmnop
export EMAIL_FROM=your-email@gmail.com
export EMAIL_TO=recipient@gmail.com

# Reload shell configuration
source ~/.bashrc  # or source ~/.zshrc
```

### Using .env File (Development Only)

**WARNING:** Never commit `.env` files to version control!

Create `.env` in project root:
```bash
EMAIL_SMTP_HOST=smtp.gmail.com
EMAIL_SMTP_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=abcdefghijklmnop
EMAIL_FROM=your-email@gmail.com
EMAIL_TO=recipient@gmail.com
```

Add to `.gitignore`:
```
.env
*.env
```

**Load before running:**
```bash
# Linux/macOS
export $(cat .env | xargs)
java -jar target/daily-task-orchestrator.jar

# Windows (PowerShell)
Get-Content .env | ForEach-Object {
    $name, $value = $_.split('=')
    Set-Item -Path "env:$name" -Value $value
}
java -jar target/daily-task-orchestrator.jar
```

---

## Step 4: Verify Configuration

Test that your environment variables are correctly set.

### Windows (PowerShell)

```powershell
echo $env:EMAIL_SMTP_HOST
echo $env:EMAIL_USERNAME
echo $env:EMAIL_PASSWORD  # Should show your app password
```

### Linux/macOS (Bash)

```bash
echo $EMAIL_SMTP_HOST
echo $EMAIL_USERNAME
echo $EMAIL_PASSWORD  # Should show your app password
```

**Expected output:**
```
smtp.gmail.com
your-email@gmail.com
abcdefghijklmnop
```

**If empty:** Variables not set - repeat Step 3

---

## Step 5: Run the Application

Build and run the Daily Task Orchestrator with email notifications enabled.

### Build the Application

```bash
# Navigate to project root
cd daily-task-orchestrator

# Clean build with tests
mvn clean package

# Expected output:
# BUILD SUCCESS
# Tests run: 314, Failures: 0, Errors: 0, Skipped: 24
```

### Run with Email Notifications

```bash
java -jar target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar
```

**Expected console output:**
```
INFO  c.d.core.config.AppConfig - Email notifier initialized: SMTP smtp.gmail.com:587, recipient: recipient@gmail.com
INFO  c.d.adapters.notifiers.EmailTaskNotifier - Generating email for 5 tasks
INFO  c.d.adapters.notifiers.SmtpEmailSender - Email sent to recipient@gmail.com
INFO  c.d.adapters.notifiers.EmailTaskNotifier - Email notification sent successfully to recipient@gmail.com
```

**If errors occur:** See Troubleshooting section below

---

## Step 6: Visual Inspection Checklist

Check your inbox and verify the email is correctly formatted.

### Gmail Web (Desktop)

- [ ] Email appears in inbox (not spam/promotions folder)
- [ ] Subject: "Daily Task Summary: X task(s)" or "⚠️ Daily Task Summary: X task(s) (Y overdue)"
- [ ] CRITICAL tasks: Red left border + red badge with white text
- [ ] HIGH tasks: Orange left border + orange badge with white text
- [ ] MEDIUM tasks: Yellow left border + yellow badge with dark text
- [ ] LOW tasks: Green left border + green badge with dark text
- [ ] Overdue tasks: Red "⚠️ OVERDUE: [deadline]" text
- [ ] Gmail links: Click opens correct email in new tab
- [ ] Recommendations: Green border, bullet points (if present)
- [ ] Footer: Timestamp "Generated on [date/time] by Daily Task Orchestrator"
- [ ] Layout: Centered, max-width 800px, proper padding

### Gmail Mobile App (iOS/Android)

- [ ] Email displays without horizontal scroll
- [ ] Priority badges readable (not overlapping text)
- [ ] Gmail links tappable (open in Gmail app)
- [ ] Text size appropriate (no tiny fonts)

### Other Email Clients (Optional)

Test in Outlook, Apple Mail, or Thunderbird if used:
- [ ] Inline CSS renders correctly
- [ ] Colors match Gmail display
- [ ] Links functional

### Accessibility

- [ ] Screen reader: Priority colors announced ("red", "orange", "yellow", "green")
- [ ] High contrast mode: Text still readable

**If any checks fail:** See Troubleshooting section

---

## Troubleshooting

### Email Not Received

**Check spam/promotions folders:**
```
1. Open Gmail
2. Check "Spam" folder
3. Check "Promotions" tab (if using tabs)
4. If found: Click "Not spam" or move to Primary inbox
```

**Verify environment variables are set:**
```bash
# Windows (PowerShell)
echo $env:EMAIL_SMTP_HOST
echo $env:EMAIL_TO

# Linux/macOS (Bash)
echo $EMAIL_SMTP_HOST
echo $EMAIL_TO
```

**Check application logs:**
```
Look for:
✅ "Email notification sent successfully"
❌ "Email notification failed"
```

### Authentication Failed (535 Error)

**Error message:**
```
ERROR c.d.adapters.notifiers.SmtpEmailSender - SMTP send failed: 535-5.7.8 Username and Password not accepted
```

**Solutions:**

1. **Verify App Password (most common issue):**
   - App Password must be 16 characters with **no spaces**
   - Example: `abcdefghijklmnop` (not `abcd efgh ijkl mnop`)
   - Regenerate if unsure: [Google App Passwords](https://myaccount.google.com/apppasswords)

2. **Check 2FA is enabled:**
   - Visit [Google Account Security](https://myaccount.google.com/security)
   - "2-Step Verification" must show "On"

3. **Verify EMAIL_USERNAME matches EMAIL_FROM:**
   ```bash
   # Both should be identical
   echo $EMAIL_USERNAME
   echo $EMAIL_FROM
   ```

4. **Try a fresh App Password:**
   - Delete old App Password in Google Account
   - Generate new one with name "Daily Task Orchestrator v2"
   - Update `EMAIL_PASSWORD` environment variable

### Timeout Error

**Error message:**
```
ERROR c.d.adapters.notifiers.SmtpEmailSender - SMTP send failed: Read timed out
```

**Solutions:**

1. **Increase timeout:**
   ```bash
   export EMAIL_TIMEOUT_MS=60000  # 60 seconds
   ```

2. **Check network connectivity:**
   ```bash
   # Test if SMTP port is reachable
   telnet smtp.gmail.com 587
   # (press Ctrl+C to exit if connection succeeds)
   ```

3. **Verify firewall rules:**
   - Corporate/university networks may block port 587
   - Try from a different network (home Wi-Fi, mobile hotspot)

4. **Try alternate port (SSL):**
   ```bash
   export EMAIL_SMTP_PORT=465  # SSL instead of STARTTLS
   ```

### Email Formatting Issues

**Colors not displaying:**
- Gmail mobile sometimes strips CSS on first load - refresh email
- Check if email client supports inline CSS (most modern clients do)

**Priority badges overlapping:**
- Device screen too narrow - rotate to landscape mode
- Update to latest Gmail app version

**Gmail links not working:**
- Links require `originalId` field from Gmail API
- Check Task objects have `originalId` populated
- If missing: Ensure Gmail integration is working correctly

### Configuration Validation Errors

**Error message:**
```
ERROR c.d.core.config.AppConfig - Email notifier configuration failed: EMAIL_SMTP_HOST environment variable is required
```

**Solution:**
```bash
# Windows (PowerShell) - Persistent
[System.Environment]::SetEnvironmentVariable('EMAIL_SMTP_HOST', 'smtp.gmail.com', 'User')

# Restart terminal, then verify
echo $env:EMAIL_SMTP_HOST
```

**Invalid email format:**
```
ERROR c.d.core.config.AppConfig - Email notifier configuration failed: EMAIL_FROM must be a valid email address
```

**Solution:**
```bash
# Email must match pattern: user@domain.com
export EMAIL_FROM=your-email@gmail.com  # ✅ Valid
# Not: your-email  # ❌ Invalid
```

### Google Workspace Restrictions

**App Passwords not available:**
- Your Google Workspace admin may have disabled App Passwords
- Contact your admin to enable "Less secure apps" or App Passwords
- Alternative: Use OAuth2 (requires code changes - see Phase 6+)

---

## Security Best Practices

### Protecting Your App Password

1. **Never commit to version control:**
   ```bash
   # Add to .gitignore
   echo ".env" >> .gitignore
   echo "*.env" >> .gitignore
   ```

2. **Use secrets management in production:**
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault
   - 1Password / LastPass

3. **Rotate passwords regularly:**
   - Revoke old App Passwords in Google Account every 90 days
   - Generate new ones with updated names

4. **Limit scope:**
   - App Passwords have full account access
   - Consider using a dedicated Gmail account for notifications only

5. **Monitor usage:**
   - Check [Recent security activity](https://myaccount.google.com/notifications) regularly
   - Revoke unknown App Passwords immediately

### Production Deployment

**Environment variables:**
```bash
# Good: Kubernetes secrets
kubectl create secret generic email-credentials \
  --from-literal=EMAIL_PASSWORD=abcdefghijklmnop

# Good: Docker secrets
docker secret create email_password abcdefghijklmnop

# Bad: Hardcoded in Dockerfile
ENV EMAIL_PASSWORD=abcdefghijklmnop  # ❌ Never do this
```

**Logging:**
```java
// ✅ GOOD: Mask sensitive data
logger.info("Using email: {}...{}", 
    email.substring(0, 3), email.substring(email.length() - 5));

// ❌ BAD: Log full password
logger.info("Password: {}", password);
```

---

## Alternative SMTP Providers

While Gmail is recommended for personal use, here are alternatives for production:

### SendGrid

```bash
export EMAIL_SMTP_HOST=smtp.sendgrid.net
export EMAIL_SMTP_PORT=587
export EMAIL_USERNAME=apikey
export EMAIL_PASSWORD=SG.your-sendgrid-api-key
```

**Pros:** 100 emails/day free tier, better deliverability  
**Cons:** Requires account creation, API key management

### Amazon SES

```bash
export EMAIL_SMTP_HOST=email-smtp.us-east-1.amazonaws.com
export EMAIL_SMTP_PORT=587
export EMAIL_USERNAME=your-ses-smtp-username
export EMAIL_PASSWORD=your-ses-smtp-password
```

**Pros:** Scales to millions of emails, pay-per-use  
**Cons:** AWS account required, domain verification needed

### Mailgun

```bash
export EMAIL_SMTP_HOST=smtp.mailgun.org
export EMAIL_SMTP_PORT=587
export EMAIL_USERNAME=postmaster@your-domain.mailgun.org
export EMAIL_PASSWORD=your-mailgun-smtp-password
```

**Pros:** 5,000 emails/month free, excellent API  
**Cons:** Domain ownership required

---

## FAQ

**Q: Can I send to multiple recipients?**  
A: Currently, only one recipient is supported via `EMAIL_TO`. Phase 6 will add support for comma-separated addresses.

**Q: How often are emails sent?**  
A: Once per application run. To schedule daily emails, use cron (Linux) or Task Scheduler (Windows).

**Q: Will emails end up in spam?**  
A: Using App Passwords with Gmail SMTP rarely triggers spam filters. If it happens, mark as "Not spam" once.

**Q: Can I customize the email template?**  
A: Yes - modify `HtmlContentBuilder.java` in `src/main/java/com/dailytask/adapters/notifiers/`.

**Q: What if I don't have a Gmail account?**  
A: Use any SMTP provider (SendGrid, Amazon SES, Mailgun) - just update the environment variables.

**Q: Is my App Password secure?**  
A: Yes - it's scoped to your application and can be revoked anytime without changing your main Gmail password.

**Q: Can I test without a real SMTP server?**  
A: Yes - the integration tests use GreenMail (in-memory SMTP). Run: `mvn test -Dtest=EmailTaskNotifierIntegrationTest`

---

## Next Steps

**Email working correctly?**
- ✅ Proceed to README.md for general project setup
- ✅ Explore Phase 6 roadmap: Retry logic, circuit breaker, multiple recipients

**Still having issues?**
- 📧 Check troubleshooting section above
- 📝 Review application logs for specific error messages
- 🔍 Search GitHub Issues: [daily-task-orchestrator/issues](https://github.com/yourusername/daily-task-orchestrator/issues)

---

**Document version:** 1.0.0 (Phase 5 - Email Notification System)  
**Last updated:** 2026-07-02
