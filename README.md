# Sigil Auth Mobile Apps

Native iOS and Android apps for Sigil Auth — hardware-backed cryptographic authentication.

**Platforms:**
- iOS 16.0+ (Swift + Secure Enclave)
- Android 8.0+ (Kotlin + StrongBox)

**License:** AGPL-3.0

---

## Quick Start

### Build iOS App

```bash
# Prerequisites: Xcode, xcodegen
brew install xcodegen

# Build and export IPA for TestFlight
make ios-upload
# Output: build/SigilAuth_AppStore/SigilAuth.ipa

# Or use Fastlane (uploads + delivers metadata)
export APPLE_ID=your@email.com
export SIGIL_TESTFLIGHT_APPPW=<app-specific-password>
fastlane ios_beta
```

### Build Android App

```bash
# Prerequisites: Android SDK
# Set SDK location in android/local.properties:
# sdk.dir=/path/to/android/sdk

# Build release AAB
export SIGIL_KEYSTORE_PASSWORD=<your-keystore-password>
export SIGIL_KEY_PASSWORD=<your-key-password>
make android-bundle
# Output: android/app/build/outputs/bundle/release/app-release.aab

# Or use Fastlane (uploads + delivers metadata)
fastlane android_internal
```

---

## Project Structure

```
app/
├── ios/                        # iOS app (Swift)
│   ├── Project.yml            # xcodegen configuration
│   ├── App/                   # App entry point
│   ├── Sources/               # Swift Package sources
│   │   ├── UI/               # SwiftUI views
│   │   └── Core/             # Business logic
│   ├── Tests/                # XCUITest + unit tests
│   └── fastlane/
│       ├── metadata/         # App Store metadata
│       └── screenshots/      # App Store screenshots
│
├── android/                   # Android app (Kotlin)
│   ├── app/
│   │   ├── build.gradle.kts # App module config
│   │   └── src/
│   │       ├── main/        # Kotlin sources + Compose UI
│   │       ├── test/        # Unit tests
│   │       └── androidTest/ # Instrumented tests
│   └── fastlane/
│       └── metadata/        # Play Store metadata + screenshots
│
├── Makefile                  # Build automation
├── Fastfile                  # Fastlane deployment
└── MOBILE-DEV.md            # Integration testing guide
```

---

## Development

### iOS

**Generate Xcode project:**
```bash
cd ios
xcodegen generate
open SigilAuth.xcodeproj
```

**Run tests:**
```bash
swift test --filter UITests
# Or in Xcode: Cmd+U
```

**Design tokens:**
- Location: `ios/Sources/UI/Theme/SigilColors.swift`
- Colors, spacing, typography from sigilauth.com
- All views use token values (`.sigilPrimary`, `.s6`, etc.)

### Android

**Build debug:**
```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Run tests:**
```bash
./gradlew testDebugUnitTest           # Unit tests
./gradlew connectedAndroidTest        # Instrumented tests (requires device/emulator)
```

**Design tokens:**
- Location: `android/app/src/main/kotlin/com/sigilauth/app/ui/theme/SigilColors.kt`
- Matches iOS tokens for visual consistency
- All screens use `SigilColors` and `SigilSpacing`

---

## Deployment

### TestFlight (iOS)

**Option 1: Fastlane (recommended)**
```bash
export APPLE_ID=kaity@wagmilabs.vc
export SIGIL_TESTFLIGHT_APPPW=<app-specific-password>
fastlane ios_beta
```

**Option 2: Make + manual upload**
```bash
make ios-upload
# Then upload ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa via Transporter.app
```

**What happens:**
1. `xcodegen` generates Xcode project from `ios/Project.yml`
2. `xcodebuild` archives app with Release configuration
3. IPA exported with App Store distribution profile
4. Metadata + screenshots delivered to App Store Connect
5. Binary uploaded via `xcrun altool`

**First-time setup:**
1. Create app in App Store Connect (ID: 6763482460)
2. Generate app-specific password at appleid.apple.com
3. Set `SIGIL_TESTFLIGHT_APPPW` environment variable

### Play Console Internal Testing (Android)

**Option 1: Fastlane (recommended)**
```bash
export SIGIL_KEYSTORE_PASSWORD=<keystore-password>
export SIGIL_KEY_PASSWORD=<key-password>
fastlane android_internal
```

**Option 2: Make + manual upload**
```bash
make android-bundle
# Then upload android/app/build/outputs/bundle/release/app-release.aab
# via Play Console web interface
```

**What happens:**
1. Gradle builds release AAB with ProGuard
2. App signed with release keystore (`android/app/release.keystore`)
3. Metadata + screenshots delivered to Play Console
4. AAB uploaded to Internal Testing track

**First-time setup:**
1. Create app in Play Console (package: `com.wagmilabs.sigil`)
2. Generate release keystore: `make android-keystore` (if not exists)
3. Set keystore password environment variables

---

## Metadata & Screenshots

### Update Metadata

Edit text files directly:

**iOS:**
```bash
# Edit metadata
vim ios/fastlane/metadata/en-US/description.txt

# Push to App Store Connect
fastlane ios metadata
```

**Android:**
```bash
# Edit metadata
vim android/fastlane/metadata/android/en-US/full_description.txt

# Push to Play Console
fastlane android metadata
```

### Generate Screenshots

**iOS:**
See `ios/fastlane/screenshots/README.md` for detailed instructions.

Quick method (manual):
```bash
# Boot simulator
xcrun simctl boot "iPhone 15 Pro Max"
open -a Simulator

# Build and run app, navigate to each screen
# Capture screenshot:
xcrun simctl io booted screenshot screenshots/en-US/01_pairing_6-7.png
```

**Android:**
See `android/fastlane/metadata/android/en-US/images/README.md` for detailed instructions.

Quick method (emulator):
```bash
# Start emulator
emulator -avd Pixel_6_API_33 &

# Install app
cd android && ./gradlew installDebug

# Capture screenshot:
adb exec-out screencap -p > phoneScreenshots/01_pairing.png
```

---

## Testing

### Unit Tests

```bash
# iOS
swift test

# Android
cd android && ./gradlew testDebugUnitTest
```

### UI Tests

```bash
# iOS
swift test --filter UITests
# Or: Cmd+U in Xcode

# Android (requires device/emulator)
cd android && ./gradlew connectedAndroidTest
```

### Integration Testing

Use `cli-device` for backend integration testing without physical devices.
See [MOBILE-DEV.md](./MOBILE-DEV.md) for full guide.

---

## Makefile Targets

| Target | Description |
|--------|-------------|
| `make ios-archive` | Build iOS archive |
| `make ios-export-appstore` | Export IPA with App Store profile |
| `make ios-upload` | Archive + export + upload to TestFlight |
| `make android-bundle` | Build release AAB |
| `make android-upload` | Display upload instructions |
| `make clean` | Remove build artifacts |
| `make help` | Show all targets |

## Fastlane Lanes

| Lane | Platform | Description |
|------|----------|-------------|
| `fastlane ios_beta` | iOS | Build + upload to TestFlight + deliver metadata |
| `fastlane android_internal` | Android | Build + upload to Play Internal + deliver metadata |
| `fastlane ios metadata` | iOS | Update metadata/screenshots only (no binary) |
| `fastlane android metadata` | Android | Update metadata/screenshots only (no binary) |
| `fastlane ios validate` | iOS | Validate Fastlane config without upload |
| `fastlane android validate` | Android | Validate Fastlane config without upload |
| `fastlane validate_all` | Both | Run all validation checks |

---

## Environment Variables

### Required for iOS

| Variable | Purpose | How to Get |
|----------|---------|------------|
| `APPLE_ID` | Apple Developer account email | Your Apple ID |
| `SIGIL_TESTFLIGHT_APPPW` | App-specific password | appleid.apple.com → Security → App-Specific Passwords |

### Required for Android

| Variable | Purpose | How to Get |
|----------|---------|------------|
| `SIGIL_KEYSTORE_PASSWORD` | Release keystore password | Set when generating keystore |
| `SIGIL_KEY_PASSWORD` | Release key password | Usually same as keystore password |

### Optional

| Variable | Purpose |
|----------|---------|
| `PLAY_STORE_JSON_KEY` | Service account JSON for automated uploads |

---

## Troubleshooting

### iOS build fails with "No such module 'SigilAuthCore'"

```bash
# Regenerate Xcode project
cd ios && xcodegen generate
```

### Android build fails with "SDK location not found"

```bash
# Create local.properties with SDK path
echo "sdk.dir=/Users/$(whoami)/Library/Android/sdk" > android/local.properties
```

### "Provisioning profile doesn't match" on iOS export

```bash
# Re-export with correct profile
make ios-export-appstore
```

### Screenshots not uploading

Check directory structure matches Fastlane expectations:
- iOS: `ios/fastlane/screenshots/en-US/*.png`
- Android: `android/fastlane/metadata/android/en-US/images/phoneScreenshots/*.png`

---

## Contributing

See root [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

**Code quality:**
- All views use design tokens (no hardcoded colors/spacing)
- Accessibility labels on all interactive elements
- UI tests cover critical flows
- Conventional commits

---

## Links

- **Website:** https://sigilauth.com
- **Issues:** https://github.com/sigilauth/app/issues
- **API Spec:** https://github.com/sigilauth/project (monorepo root)
- **Integration Guide:** [MOBILE-DEV.md](./MOBILE-DEV.md)

---

**License:** AGPL-3.0 • **Maintainer:** Wagmi Labs
