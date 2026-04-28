# iOS Screenshots for App Store

**Status:** Pending generation

## Required Sizes

App Store Connect requires screenshots for:

| Device | Resolution | Filename Pattern |
|--------|------------|------------------|
| 6.7" Display (iPhone 15 Pro Max) | 1290 × 2796 | `01_pairing_6-7.png` |
| 5.5" Display (iPhone 8 Plus) | 1242 × 2208 | `01_pairing_5-5.png` |
| iPad Pro 12.9" (6th gen) | 2048 × 2732 | `01_pairing_ipad.png` |

## Screenshot Sequence

Generate 4-5 screenshots showing:

1. **Pairing screen** — "Pair Device" title, QR scanner option visible
2. **Code entry** — 8-digit input boxes, clean state
3. **Approval screen** — "Approve Login?" with badge, action details
4. **Mnemonic display** — 12-word recovery phrase grid (screenshot protection warning visible)
5. **Pictogram verification** — Server pictogram with Confirm/Reject buttons

## Generation Methods

### Method 1: Fastlane Snapshot (Automated)

```bash
# Install Snapshot
bundle exec fastlane snapshot init

# Configure Snapfile with devices and scheme
# Run snapshot
bundle exec fastlane snapshot
```

### Method 2: Manual with Simulator

```bash
# Boot iPhone 15 Pro Max simulator
xcrun simctl boot "iPhone 15 Pro Max"

# Open simulator UI
open -a Simulator

# Build and run app
xcodebuild -project ios/SigilAuth.xcodeproj \
  -scheme SigilAuth \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro Max' \
  -derivedDataPath build

# Navigate to each screen in app, then:
xcrun simctl io booted screenshot screenshots/en-US/01_pairing_6-7.png

# Repeat for each screen and device size
```

### Method 3: Manual from Device

1. Build app to physical device
2. Navigate to each screen
3. Take screenshot (Volume Up + Power)
4. AirDrop to Mac
5. Resize to required dimensions using:
   ```bash
   sips -z 2796 1290 screenshot.png --out 01_pairing_6-7.png
   ```

## Status Indicators

Add these to screenshots if needed:
- Time: 9:41 AM (Apple convention)
- Signal: Full bars
- Battery: Fully charged
- Network: Wi-Fi connected

## Notes

- Screenshots must show actual app UI (not wireframes) for App Store submission
- Dark mode screenshots optional but recommended
- Localized screenshots optional (en-US is minimum)
- Max 10 screenshots per device size
