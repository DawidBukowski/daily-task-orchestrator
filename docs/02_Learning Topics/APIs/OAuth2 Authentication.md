# OAuth2 Authentication

## The Problem OAuth2 Solves

**Without OAuth2:**
- You give app your password
- App stores your password
- If app is hacked, attacker gets your password
- Problem!

**With OAuth2:**
- You log in at Google's site (not app's site)
- Google gives app a token (not password)
- App uses token to access your data
- Token can expire or be revoked
- Much safer!

## The Flow
- User clicks "Connect Gmail" ↓
- App redirects to Google login ↓
- User logs in to Google ↓
- Google asks: "Let this app read your email?" ↓
- User approves ↓
- Google gives app an auth code ↓
- App exchanges code for access token ↓
- App uses token to fetch emails
## Key Concepts

- **Access Token**: Short-lived (1 hour), used to access API
- **Refresh Token**: Long-lived, used to get new access tokens
- **Scope**: What permission (gmail.readonly = read only)
- **Redirect URI**: Where Google sends code back

## In Phase 2

We implement `GmailOAuth2Handler` that:
1. Generates auth URL for user
2. Exchanges code for tokens
3. Refreshes token when expired
4. Stores tokens securely

See: Phase 2 Step 1

Resources:
- RFC 6749: https://tools.ietf.org/html/rfc6749
- Google OAuth2: https://developers.google.com/identity/protocols/oauth2