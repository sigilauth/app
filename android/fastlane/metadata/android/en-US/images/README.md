# Android Screenshots for Play Console

**Status:** Pending generation

## Required Sizes

Play Console requires:

| Device Type | Min Size | Max Size | Directory |
|-------------|----------|----------|-----------|
| Phone | 320px | 3840px | `phoneScreenshots/` |
| 7" Tablet | 1024 x 600 | 7680 x 4320 | N/A (optional) |
| 10" Tablet | 1024 x 768 | 7680 x 4320 | `tenInchScreenshots/` |

**Recommended:**
- Phone: 1080 x 1920 (portrait) or 1920 x 1080 (landscape)
- 10" Tablet: 1200 x 1920 or 1920 x 1200

## Screenshot Sequence

Generate 4-5 screenshots showing:

1. **Registration screen** — "Pair Device" title with QR/Code/Manual options
2. **Code entry** — 8-digit input boxes
3. **Approval screen** — "Approve Login?" with badge and action card
4. **QR scanner** — Camera view with alignment instructions
5. **Settings** (if implemented) — Paired servers list

## Generation Methods

### Method 1: Android Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator (Pixel 6 recommended for phone screenshots)
emulator -avd Pixel_6_API_33 &

# Build and install app
cd android
./gradlew installDebug

# Launch app
adb shell am start -n com.wagmilabs.sigil/.MainActivity

# Navigate to screen, then capture
adb exec-out screencap -p > phoneScreenshots/01_pairing.png

# Repeat for each screen
```

### Method 2: Physical Device

```bash
# Connect device via USB
adb devices

# Build and install
cd android
./gradlew installDebug

# Navigate to each screen in app
# Capture screenshot
adb exec-out screencap -p > phoneScreenshots/01_pairing.png
```

### Method 3: Fastlane Screengrab

```bash
# Install screengrab
bundle exec fastlane screengrab init

# Configure Screengrabfile with locales and device types
# Run screengrab
bundle exec fastlane screengrab
```

## Filename Convention

Use descriptive names:
- `01_pairing_qr.png`
- `02_code_entry.png`
- `03_approval_screen.png`
- `04_qr_scanner.png`
- `05_settings.png`

## Image Requirements

- Format: PNG or JPEG
- Must show actual app UI (no mockups for initial submission)
- No device frames required
- Supported aspect ratios: 16:9 to 2:1
- Min 2 screenshots, max 8 per device type

## Feature Graphic (Required)

Create `featureGraphic.png`:
- Size: 1024 x 500 px exactly
- Format: PNG or JPEG
- Shows app branding + key features
- No borders or device frames

## Notes

- Screenshots must match current app version
- Dark theme screenshots optional
- Localized screenshots optional (en-US minimum)
- Play Console validates dimensions automatically
