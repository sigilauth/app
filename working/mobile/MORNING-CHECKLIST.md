# Morning Deployment Checklist

**Goal:** Ship Sigil Auth to TestFlight (iOS) + Play Console Internal Testing (Android)  
**Time estimate:** 15 minutes iOS, 30 minutes Android (if SDK installed)  
**Date:** 2026-04-26

---

## Pre-Flight

- [ ] All commits pushed to `main`
- [ ] `working/mobile/build-log.md` reviewed (iOS IPA validated ✓)
- [ ] Coffee ready

---

## iOS → TestFlight (5-10 minutes)

**BLOCKER:** Need new IPA with latest assets (icons + launch screen added after last build)

### Step 1: Rebuild IPA with Latest Assets

```bash
cd /Volumes/Expansion/src/sigilauth/app
make ios-upload
```

**What it does:**
1. Runs `xcodegen generate` (creates Xcode project from Project.yml)
2. Runs `xcodebuild archive` (builds .xcarchive)
3. Runs `xcodebuild -exportArchive` (creates .ipa with App Store profile)
4. **Stops at upload** (needs credentials)

**Output:** `~/Desktop/SigilAuth_AppStore/SigilAuth.ipa` (~300KB, includes new assets)

**Time:** 2-3 minutes

---

### Step 2A: Upload with App-Specific Password (Quick Method)

**2A.1. Generate Password**

1. Open https://appleid.apple.com
2. Sign in: `kaity@wagmilabs.vc`
3. Click "Sign-In and Security" (left sidebar)
4. Scroll to "App-Specific Passwords" → click "Generate"
5. Label: "Sigil Auth TestFlight" → Create
6. **Copy password immediately** (shown once, format: xxxx-xxxx-xxxx-xxxx)

**2A.2. Set Environment & Upload**

```bash
export APPLE_ID=kaity@wagmilabs.vc
export SIGIL_TESTFLIGHT_APPPW="xxxx-xxxx-xxxx-xxxx"  # paste password here

# Upload
xcrun altool --upload-app \
  -f ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa \
  -u $APPLE_ID \
  -p $SIGIL_TESTFLIGHT_APPPW \
  --type ios
```

**What happens:** Upload starts, shows progress %, completes in 30-60 seconds

**Success message:** `No errors uploading 'SigilAuth.ipa'`

**DONE:** Skip to Step 4

---

### Step 2B: Upload with API Key (Better for Long-Term)

**Why better:** API keys never expire, app-specific passwords expire after 90 days

**2B.1. Generate API Key**

1. Open https://appstoreconnect.apple.com/access/api
2. Sign in: `kaity@wagmilabs.vc`
3. Click "Keys" tab (left sidebar under "Users and Access")
4. Click blue "+" button (top right)
5. **Name:** "Sigil Auth CLI"
6. **Access:** "App Manager" (minimum for uploads)
7. Click "Generate"
8. **Download .p8 file immediately** (shown once, can't re-download)
   - Filename: `AuthKey_XXXXXXXXXX.p8` (X = your key ID)
9. **Copy Issuer ID** (UUID at top of page, format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
10. **Copy Key ID** (10 characters shown next to key name, format: `ABCD123456`)

**2B.2. Install Key**

```bash
# Create directory
mkdir -p ~/.private_keys

# Move downloaded key (replace XXXXXXXXXX with your Key ID)
mv ~/Downloads/AuthKey_XXXXXXXXXX.p8 ~/.private_keys/

# Set permissions (important!)
chmod 600 ~/.private_keys/AuthKey_XXXXXXXXXX.p8
```

**2B.3. Upload with API Key**

```bash
# Set variables (replace with your actual IDs)
export APPSTORE_API_KEY_ID="ABCD123456"
export APPSTORE_API_ISSUER="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

# Upload
xcrun altool --upload-app \
  -f ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa \
  --type ios \
  --apiKey $APPSTORE_API_KEY_ID \
  --apiIssuer $APPSTORE_API_ISSUER
```

**Success message:** `No errors uploading 'SigilAuth.ipa'`

**Future uploads:** Just run the upload command (key never expires)

---

### Step 3: Wait for Processing

**Apple's side:** 5-10 minutes to process uploaded IPA

1. Open https://appstoreconnect.apple.com
2. My Apps → Sigil Auth (or search "6763482460")
3. TestFlight tab (top navigation)
4. Watch "Processing" → changes to version number when ready

**Email notification:** You'll receive "Your build is ready for testing" email

### Step 4: Add Testers (While Build Processes)

**4.1. Create Internal Testing Group (if not exists)**

1. App Store Connect → Sigil Auth → TestFlight tab
2. Left sidebar → "Internal Testing" (under "INTERNAL TESTING")
3. If no group exists → click blue "+" → "Add Internal Group"
   - **Group Name:** "Wagmi Labs Team"
   - Click "Create"

**4.2. Add Testers to Group**

1. Click group name ("Wagmi Labs Team")
2. Click "Testers" tab
3. Click blue "+" next to "Internal Testers"
4. Check boxes next to names:
   - ✓ Kaity (kaity@wagmilabs.vc)
   - ✓ (Add other team members)
5. Click "Add" button (bottom right)

**4.3. Enable Build for Group**

1. Still in "Wagmi Labs Team" group
2. Click "Builds" tab
3. Click blue "+" next to builds
4. Select version 0.1.0 (build 1) → Add
5. Build appears in list

**Email sent:** Testers receive "You're Invited to Test Sigil Auth" email

**Status:** ✓ Testers can install via TestFlight app on iPhone

---

## Android → Play Console Internal Testing (20-30 minutes first time)

### Step 1: Install Android SDK (if not installed)

**Check if already installed:**
```bash
ls ~/Library/Android/sdk/platforms/android-34 2>&1
```

**If exists:** Skip to Step 2

**If not exists:**

**1.1. Download Android Studio**

1. Open https://developer.android.com/studio
2. Click blue "Download Android Studio" button
3. Accept terms → Download
4. File: `android-studio-2024.1.1.12-mac_arm.dmg` (~1.2GB)

**1.2. Install Android Studio**

1. Open downloaded .dmg file
2. Drag "Android Studio" to Applications folder
3. Open Android Studio from Applications
4. First launch wizard:
   - "Do not import settings" → OK
   - "Send usage statistics" → Don't send (or Send, your choice) → Next
   - "Install Type" → Standard → Next
   - "UI Theme" → (your choice) → Next
   - "Verify Settings" → Finish
   - Wait for SDK download (5-10 minutes, ~3GB)

**1.3. Install Required SDK Versions**

1. Android Studio welcome screen → "More Actions" → "SDK Manager"
   OR: Top menu → Tools → SDK Manager
2. **SDK Platforms tab:**
   - ✓ Check "Android 14.0 (UpsideDownCake)" — API Level 34
   - ✓ Check "Show Package Details" (bottom right)
     - Under Android 14.0: ✓ "Android SDK Platform 34"
3. **SDK Tools tab:**
   - ✓ Check "Show Package Details" (bottom right)
   - Find "Android SDK Build-Tools"
     - ✓ Check "34.0.0"
4. Click "Apply" button (bottom right)
5. Confirm download (shows size ~500MB)
6. Click "OK" → wait for download + install (2-3 minutes)
7. Click "Finish" when done

**1.4. Verify Installation**

```bash
ls ~/Library/Android/sdk/platforms/android-34
# Expected output: android.jar  build.prop  data  framework.aidl  ...

~/Library/Android/sdk/build-tools/34.0.0/aapt version
# Expected output: Android Asset Packaging Tool, v0.2-...
```

### 2. Configure SDK Path
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > /Volumes/Expansion/src/sigilauth/app/android/local.properties
```

### 3. Set Keystore Passwords
```bash
export SIGIL_KEYSTORE_PASSWORD=<password>
export SIGIL_KEY_PASSWORD=<password>
```

**Note:** Keystore at `app/android/app/release.keystore`. If doesn't exist, run `make android-keystore` first.

### 4. Build Release AAB
```bash
cd /Volumes/Expansion/src/sigilauth/app
make android-bundle
```

**Output:** `android/app/build/outputs/bundle/release/app-release.aab`  
**Time:** 2-5 minutes first build (downloads Gradle dependencies)

**Alternative (Fastlane):**
```bash
cd /Volumes/Expansion/src/sigilauth/app
bundle exec fastlane android_internal
```

### Step 5: Upload AAB to Play Console

**5.1. Navigate to Internal Testing**

1. Open https://play.google.com/console
2. Sign in: kaity@wagmilabs.vc
3. Click "Sigil Auth" app tile (or search "com.wagmilabs.sigil")
4. Left sidebar → "Testing" → "Internal testing"

**5.2. Create Release**

1. Click "Create new release" button (top right)
2. **App bundles** section:
   - Click "Upload" button
   - Navigate to: `/Volumes/Expansion/src/sigilauth/app/android/app/build/outputs/bundle/release/`
   - Select: `app-release.aab`
   - Wait for upload (5-10 seconds, ~15MB)
   - ✓ "app-release.aab uploaded successfully"

**5.3. Fill Release Details**

1. **Release name:** "0.1.0 (Initial Internal Test)"
2. **Release notes:**
   - Click "Add release notes"
   - Language: en-US
   - Paste content from: `android/store-listing/release-notes-v0.1.0.txt`
   - Click "Save"

**5.4. Review & Roll Out**

1. Click "Next" button (bottom right)
2. Review summary screen → click "Start rollout to Internal testing"
3. Confirm dialog → "Rollout"

**Processing:** 10-30 minutes for Play Console to process AAB

**Status:** "Pending publication" → changes to "Available to testers"

### Step 6: Add Testers (While Build Processes)

**6.1. Create Tester List (if not exists)**

1. Still in Internal testing page
2. Scroll to "Testers" section (middle of page)
3. Click "Create email list" tab
4. **List name:** "Wagmi Labs Team"
5. **Add email addresses:** (one per line)
   ```
   kaity@wagmilabs.vc
   (add other team members)
   ```
6. Click "Save changes"

**6.2. Enable List for Internal Testing**

1. Still in "Testers" section
2. Under "Choose how to add testers" → select "Email lists"
3. ✓ Check "Wagmi Labs Team"
4. Click "Save" (bottom of section)

**Copy opt-in URL:**
1. After saving, "Copy link" button appears next to list name
2. Click "Copy link"
3. Send link to testers (they click to opt-in before receiving app)

**Email notification:** After testers opt-in, they receive link to download from Play Store

**Status:** ✓ Testers can install once build is published (~30 min)

---

## Post-Deployment Verification

### iOS TestFlight
```bash
# On iPhone:
1. Open TestFlight app
2. Accept invite
3. Install Sigil Auth
4. Launch → test pairing flow
```

### Android Internal Testing
```bash
# On Android device:
1. Open email invite
2. Accept testing invitation
3. Download from Play Store
4. Launch → test pairing flow
```

---

## Screenshots (Optional — can upload later)

If time permits, generate screenshots for store listings.

### iOS Screenshots
```bash
cd /Volumes/Expansion/src/sigilauth/app/ios/fastlane/screenshots
# Follow README.md instructions
# Upload via: fastlane ios metadata
```

### Android Screenshots
```bash
cd /Volumes/Expansion/src/sigilauth/app/android/fastlane/metadata/android/en-US/images
# Follow README.md instructions
# Upload via: fastlane android metadata
```

**Note:** Screenshots not required for TestFlight/Internal Testing. Can be added before public release.

---

## Troubleshooting

### iOS: "altool: command not found"
**Fix:** Install Xcode Command Line Tools
```bash
xcode-select --install
```

### iOS: "Invalid credentials"
**Fix:** Regenerate app-specific password, ensure no typos in `$SIGIL_TESTFLIGHT_APPPW`

### Android: "SDK location not found"
**Fix:** Verify `android/local.properties` has correct path
```bash
cat android/local.properties
# Should show: sdk.dir=/Users/kaity/Library/Android/sdk
```

### Android: "Keystore not found"
**Fix:** Generate keystore
```bash
cd /Volumes/Expansion/src/sigilauth/app
make android-keystore
# Follow prompts, save password
```

### Fastlane: "Gemfile not found"
**Fix:** Install dependencies
```bash
cd /Volumes/Expansion/src/sigilauth/app
bundle install
```

---

## Success Criteria

- [ ] iOS build appears in App Store Connect → TestFlight
- [ ] Android build appears in Play Console → Internal Testing
- [ ] You can install on your device via TestFlight/Play Store
- [ ] App launches and displays Home screen
- [ ] "Pair Device" flow works (QR scan or manual entry)

**Done:** Ship notification sent. Coffee deserved.

---

**Last Updated:** 2026-04-26  
**Next:** Generate screenshots, invite beta testers, iterate on feedback
