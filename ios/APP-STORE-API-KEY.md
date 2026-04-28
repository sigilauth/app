# App Store Connect API Key Setup

**Purpose:** Non-expiring API authentication for TestFlight uploads and App Store management

**Advantage over app-specific password:**
- Never expires (password requires regeneration every 90 days)
- More granular permissions
- Scriptable CI/CD
- No 2FA prompts

---

## Generate API Key

### 1. Create Key in App Store Connect

**URL:** https://appstoreconnect.apple.com/access/api

**Steps:**
1. Sign in with Apple ID: `kaity@wagmilabs.vc`
2. Navigate to: Users and Access → Keys
3. Click "+" to generate new key
4. Name: "Sigil Auth CLI Upload"
5. Access: **App Manager** (minimum for uploading builds)
6. Click "Generate"

**Download:** `.p8` file downloads immediately (only shown once — cannot re-download)

**Save as:** `AuthKey_XXXXXXXXXX.p8` (where X = key ID)

**Note these values:**
- **Issuer ID:** UUID shown at top of Keys page (e.g., `69a6de8f-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
- **Key ID:** 10-character alphanumeric (e.g., `ABCD123456`)

---

## Install Key Locally

### 2. Store Key File

**Recommended location:** `~/.appstoreconnect/api/`

```bash
mkdir -p ~/.appstoreconnect/api
mv ~/Downloads/AuthKey_XXXXXXXXXX.p8 ~/.appstoreconnect/api/
chmod 600 ~/.appstoreconnect/api/AuthKey_XXXXXXXXXX.p8
```

**Alternative location:** `~/.private_keys/` (Fastlane default)

```bash
mkdir -p ~/.private_keys
mv ~/Downloads/AuthKey_XXXXXXXXXX.p8 ~/.private_keys/
chmod 600 ~/.private_keys/AuthKey_XXXXXXXXXX.p8
```

---

## Configure Fastlane

### 3. Create Fastlane Appfile

**Location:** `ios/fastlane/Appfile` (create if doesn't exist)

```ruby
# App identifier
app_identifier "com.wagmilabs.sigil"

# Apple ID (for web login only, not used with API key)
apple_id "kaity@wagmilabs.vc"

# Team ID
team_id "CVBUNQ5UY3"

# App Store Connect API Key
api_key_path "~/.appstoreconnect/api/AuthKey_XXXXXXXXXX.p8"
api_key({
  key_id: "XXXXXXXXXX",
  issuer_id: "69a6de8f-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  key_filepath: "~/.appstoreconnect/api/AuthKey_XXXXXXXXXX.p8",
  duration: 1200,
  in_house: false
})
```

**Replace:**
- `XXXXXXXXXX` → your Key ID
- `69a6de8f-xxxx...` → your Issuer ID

---

## Update Upload Commands

### 4. Use API Key with xcrun altool

**Old method (app-specific password):**
```bash
xcrun altool --upload-app \
  -f ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa \
  -u kaity@wagmilabs.vc \
  -p $SIGIL_TESTFLIGHT_APPPW \
  --type ios
```

**New method (API key):**
```bash
xcrun altool --upload-app \
  -f ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa \
  --type ios \
  --apiKey XXXXXXXXXX \
  --apiIssuer 69a6de8f-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**Note:** API key file must be at `~/.private_keys/AuthKey_XXXXXXXXXX.p8` for altool

### 5. Update Makefile

**Location:** `app/Makefile`

**Add after existing ios-upload target:**

```makefile
ios-upload-api:
	@echo "📦 Uploading IPA to App Store Connect (API key)..."
	xcrun altool --upload-app \
	  -f ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa \
	  --type ios \
	  --apiKey $(APPSTORE_API_KEY_ID) \
	  --apiIssuer $(APPSTORE_API_ISSUER)

.PHONY: ios-upload-api
```

**Usage:**
```bash
export APPSTORE_API_KEY_ID=XXXXXXXXXX
export APPSTORE_API_ISSUER=69a6de8f-xxxx-xxxx-xxxx-xxxxxxxxxxxx
make ios-upload-api
```

### 6. Update Fastlane

Fastlane automatically uses `api_key` from `Appfile` if present. No code changes needed.

```bash
cd /Volumes/Expansion/src/sigilauth/app
fastlane ios_beta  # Uses API key if Appfile configured
```

---

## Verification

### 7. Test API Key

**Validate credentials without uploading:**

```bash
xcrun altool --list-apps \
  --apiKey XXXXXXXXXX \
  --apiIssuer 69a6de8f-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**Expected output:** List of apps in account, including Sigil Auth (6763482460)

**If fails:** Check key ID, issuer ID, file path, file permissions (should be 600)

---

## Security

**Key file permissions:**
```bash
ls -l ~/.appstoreconnect/api/AuthKey_XXXXXXXXXX.p8
# Should show: -rw------- (600)
```

**Backup key file:** Store encrypted copy in password manager or secure vault

**Revoke key:** App Store Connect → Users and Access → Keys → Revoke (cannot undo)

**Rotate keys:** Generate new key, update Appfile, revoke old key

---

## Troubleshooting

**Error: "Unable to find API key"**
- Check file path in Appfile matches actual location
- Verify key file has .p8 extension
- Ensure no typos in key ID or issuer ID

**Error: "API key expired"**
- API keys don't expire unless revoked
- Check key hasn't been revoked in App Store Connect

**Error: "Insufficient permissions"**
- Key must have "App Manager" role minimum
- Check role in App Store Connect → Keys

---

## CI/CD Integration

For GitHub Actions / CI pipelines:

```yaml
- name: Upload to TestFlight
  env:
    APPSTORE_API_KEY_ID: ${{ secrets.APPSTORE_API_KEY_ID }}
    APPSTORE_API_ISSUER: ${{ secrets.APPSTORE_API_ISSUER }}
    APPSTORE_API_KEY_B64: ${{ secrets.APPSTORE_API_KEY_B64 }}
  run: |
    echo "$APPSTORE_API_KEY_B64" | base64 -d > ~/.private_keys/AuthKey_$APPSTORE_API_KEY_ID.p8
    chmod 600 ~/.private_keys/AuthKey_$APPSTORE_API_KEY_ID.p8
    make ios-upload-api
```

**Secrets to add to GitHub:**
- `APPSTORE_API_KEY_ID` = Key ID (e.g., `ABCD123456`)
- `APPSTORE_API_ISSUER` = Issuer ID (UUID)
- `APPSTORE_API_KEY_B64` = Base64-encoded .p8 file:
  ```bash
  base64 -i ~/.appstoreconnect/api/AuthKey_XXXXXXXXXX.p8 | pbcopy
  ```

---

**Last Updated:** 2026-04-26  
**Contact:** admin@wagmilabs.vc
