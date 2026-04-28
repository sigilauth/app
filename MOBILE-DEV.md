# Mobile Development & Integration Testing Guide

**Audience:** Developers integrating Sigil Auth into their applications

This guide covers testing your Sigil Auth integration using the iOS/Android mobile apps alongside the `cli-device` backend testing tool.

---

## Overview

**User flow:** End users authenticate through the native mobile apps (iOS/Android)  
**Integration testing:** Backend developers test with `cli-device` (Go CLI tool)  
**Bridge:** Both share the same protocol (see `api/openapi.yaml`)

---

## Mobile Apps (End Users)

### iOS App (TestFlight)

**Bundle ID:** `com.wagmilabs.sigil`  
**Minimum version:** iOS 16.0  
**Capabilities:** Push Notifications, Keychain Access, Camera (for QR)

**TestFlight installation:**
1. Install TestFlight from App Store
2. Open TestFlight invite link (distributed by Wagmi Labs)
3. Install Sigil Auth beta
4. Launch and complete onboarding

**Features:**
- 📱 Device pairing via QR code, 8-digit code, or manual entry
- 🔐 Push-based authentication approval with biometric gate
- 💾 Mnemonic backup for device recovery
- 🖼️ Pictogram verification for TOFU trust

### Android App (Play Internal Testing)

**Package:** `com.wagmilabs.sigil`  
**Minimum version:** Android 8.0 (API 26)  
**Permissions:** Camera, Notifications, Biometric

**Installation:**
1. Join internal testing track (invite from Play Console)
2. Open Play Store link
3. Install Sigil Auth (internal test)
4. Launch and complete onboarding

**Features:** Same as iOS (platform parity)

---

## Backend Testing with `cli-device`

For backend/integration testing without physical devices, use the `cli-device` CLI tool.

**Repository:** `github.com/sigilauth/cli-device`  
**Purpose:** Headless device simulation for CI/CD, local testing, and automation

### Installation

```bash
# From source (requires Go 1.21+)
git clone https://github.com/sigilauth/cli-device
cd cli-device
make install

# Or download pre-built binary
curl -LO https://github.com/sigilauth/cli-device/releases/latest/download/cli-device-$(uname -s)-$(uname -m)
chmod +x cli-device-*
sudo mv cli-device-* /usr/local/bin/cli-device
```

### Quick Start

```bash
# 1. Initialize device identity (generates keypair in ~/.sigil/)
cli-device init

# 2. Pair with your server
cli-device pair --url https://your-server.com

# 3. Listen for challenges (keeps process running)
cli-device listen

# 4. In another terminal, respond to challenges
cli-device respond --challenge-id <id> --approve

# 5. Test MPA (multi-party auth)
cli-device mpa-respond --challenge-id <id> --approve

# 6. Decrypt ECIES payload
cli-device decrypt --payload <base64>

# 7. Show device info
cli-device whoami

# 8. Unpair from server
cli-device unpair --server https://your-server.com
```

### Integration Test Workflow

**Typical backend integration test:**

1. Start your Sigil-integrated backend locally
2. Run `cli-device init` (first time only)
3. Run `cli-device pair --url http://localhost:3000` to register the device
4. Backend receives device public key → stores in database
5. Trigger an authentication request from your backend
6. `cli-device listen` receives challenge via WebSocket or polling
7. `cli-device respond --approve` signs the challenge
8. Backend verifies signature → grants access

**Example (pseudo-test):**

```bash
#!/bin/bash
# test-auth-flow.sh

# Start backend
./your-backend &
BACKEND_PID=$!

# Initialize device
cli-device init --force

# Pair device
cli-device pair --url http://localhost:3000 --qr

# Listen for challenges in background
cli-device listen &
LISTEN_PID=$!

# Trigger auth request
curl -X POST http://localhost:3000/api/auth/request \
  -d '{"device_fingerprint": "$(cli-device whoami | jq -r .fingerprint)"}'

# Auto-approve challenges
sleep 2
cli-device respond --latest --approve

# Verify auth succeeded
curl http://localhost:3000/api/auth/status | grep "authenticated"

# Cleanup
kill $LISTEN_PID $BACKEND_PID
cli-device unpair --all
```

---

## Protocol Compatibility

Both mobile apps and `cli-device` implement the same protocol:

| Feature | Mobile App | cli-device |
|---------|------------|------------|
| ECDSA P-256 keypair | Secure Enclave/StrongBox | `~/.sigil/keys.json` |
| Pairing (QR/Code/Manual) | ✅ All 3 transports | ✅ QR + Manual |
| Push challenges | APNs/FCM | WebSocket fallback |
| Biometric gate | Face ID / Fingerprint | Auto-approve flag |
| MPA (M-of-N) | ✅ Multi-device | ✅ Single device |
| ECIES decrypt | ✅ | ✅ |
| Mnemonic backup | ✅ BIP39 | ✅ (import/export) |

**Key difference:** Mobile apps require user interaction (biometric approval). `cli-device` can auto-approve for automated testing.

---

## Testing Your Integration

### Step 1: Local Development

Use `cli-device` for fast iteration:

```bash
# Pair once
cli-device pair --url http://localhost:3000

# Keep listen running in a terminal
cli-device listen

# Trigger auth from your backend code
# cli-device automatically logs challenges
# Manually approve with: cli-device respond --latest --approve
```

### Step 2: Staging/CI Testing

Use `cli-device` in CI pipelines:

```yaml
# .github/workflows/integration-test.yml
- name: Install cli-device
  run: |
    curl -LO https://github.com/sigilauth/cli-device/releases/latest/download/cli-device-linux-x86_64
    chmod +x cli-device-linux-x86_64
    sudo mv cli-device-linux-x86_64 /usr/local/bin/cli-device

- name: Test auth flow
  run: |
    cli-device init
    cli-device pair --url ${{ secrets.STAGING_URL }}
    cli-device listen &
    LISTEN_PID=$!
    
    # Run your integration tests here
    npm test -- integration/auth.test.js
    
    kill $LISTEN_PID
```

### Step 3: Production UAT (User Acceptance Testing)

Use real mobile apps:

1. Invite internal testers to TestFlight (iOS) or Play Internal Testing (Android)
2. Testers install app and pair with your production server
3. Trigger authentication requests from your backend
4. Testers approve via biometric gate on their devices
5. Verify auth flow completes successfully

---

## Mobile App → Backend Flow

**High-level sequence:**

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│ Mobile App  │         │ Your Backend │         │ Sigil Server │
└──────┬──────┘         └───────┬──────┘         └───────┬──────┘
       │                        │                        │
       │ 1. Scan QR / Enter Code│                        │
       ├───────────────────────>│                        │
       │                        │                        │
       │ 2. Device Public Key + │                        │
       │    Server URL          │                        │
       │<───────────────────────┤                        │
       │                        │                        │
       │ 3. Store device_pk in  │                        │
       │    your DB             │                        │
       │                        │                        │
       │                        │ 4. User logs in (web)  │
       │                        │<───────────────────────┤
       │                        │                        │
       │                        │ 5. Send challenge to   │
       │                        │    device via push     │
       │<───────────────────────┼────────────────────────┤
       │                        │                        │
       │ 6. Biometric approval  │                        │
       │    + Sign challenge    │                        │
       ├───────────────────────>│                        │
       │                        │                        │
       │                        │ 7. Verify signature    │
       │                        ├───────────────────────>│
       │                        │                        │
       │                        │ 8. Grant session       │
       │                        │<───────────────────────┤
       │                        │                        │
```

**Your backend responsibilities:**
- Store `device_public_key` per user (received during pairing)
- Generate signed challenges when auth requested
- Push challenge to device (via Sigil relay or direct)
- Verify signed response using stored public key
- Grant session on valid signature

**Mobile app responsibilities:**
- Generate keypair in Secure Enclave / StrongBox
- Send public key during pairing
- Receive challenges via push
- Show approval UI with biometric gate
- Sign challenge and return to backend

---

## Troubleshooting

### Mobile app not receiving challenges

**Check:**
1. Push notification permissions granted
2. App registered with APNs/FCM
3. Relay service configured correctly (`relay.sigilauth.com` or your own)
4. Device fingerprint matches in your database

**Debug:**
```bash
# Check relay registration
curl https://relay.sigilauth.com/v1/devices/<fingerprint> \
  -H "Authorization: Bearer <relay-token>"

# Verify push token is registered
```

### `cli-device` WebSocket connection failing

**Check:**
1. Relay WebSocket endpoint accessible
2. Firewall allows outbound WSS connections
3. Device paired successfully (`cli-device whoami` shows server)

**Debug:**
```bash
# Verbose logging
cli-device listen --verbose

# Test relay connectivity
curl https://relay.sigilauth.com/health
```

### Signature verification fails

**Check:**
1. Backend using correct device public key from pairing
2. Challenge format matches protocol spec (`api/schemas/challenge.json`)
3. Challenge not expired (default 5min TTL)
4. Server signature valid (mutual verification)

**Debug:**
```bash
# Show device public key
cli-device whoami | jq -r .public_key

# Manually verify signature
cli-device verify --challenge <base64> --signature <base64>
```

---

## API Reference

Full protocol specification:  
📄 [`api/openapi.yaml`](../api/openapi.yaml)

Key endpoints your backend implements:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/sigil/pair` | POST | Accept device registration |
| `/sigil/challenge` | POST | Receive signed challenge response |
| `/sigil/devices/:fingerprint` | GET | Query device info |
| `/sigil/devices/:fingerprint` | DELETE | Unpair device |

---

## Support & Feedback

- 🐛 **Issues:** [github.com/sigilauth/app/issues](https://github.com/sigilauth/app/issues)
- 💬 **Discussions:** [github.com/sigilauth/app/discussions](https://github.com/sigilauth/app/discussions)
- 📧 **Email:** support@sigilauth.com

---

**License:** AGPL-3.0 (mobile apps), Apache-2.0 (protocol spec)
