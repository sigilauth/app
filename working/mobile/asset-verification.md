# Mobile Asset Bundle Verification

**Date:** 2026-04-26  
**Purpose:** Verify iOS IPA and Android AAB contain required resources

---

## iOS IPA Verification

### Binary Architecture

**Command:**
```bash
unzip -p ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa Payload/SigilAuth.app/SigilAuth | file -
```

**Expected:** Mach-O universal binary with arm64 (device) and possibly x86_64 (simulator)

**Note:** Current IPA built 2026-04-26 11:54 — BEFORE LaunchScreen and app icons were added. Needs rebuild.

### Required Resources Checklist

**App Icons:**
- [ ] AppIcon.appiconset/AppIcon-20@2x.png (40x40)
- [ ] AppIcon.appiconset/AppIcon-29@2x.png (58x58)
- [ ] AppIcon.appiconset/AppIcon-29@3x.png (87x87)
- [ ] AppIcon.appiconset/AppIcon-40@2x.png (80x80)
- [ ] AppIcon.appiconset/AppIcon-40@3x.png (120x120)
- [ ] AppIcon.appiconset/AppIcon-60@2x.png (120x120)
- [ ] AppIcon.appiconset/AppIcon-60@3x.png (180x180)
- [ ] AppIcon.appiconset/AppIcon-1024.png (1024x1024 - App Store)

**Launch Screen:**
- [ ] LaunchScreen.storyboard
- [ ] LaunchLogo.imageset/LaunchLogo@1x.png
- [ ] LaunchLogo.imageset/LaunchLogo@2x.png
- [ ] LaunchLogo.imageset/LaunchLogo@3x.png

**Firebase:**
- [x] GoogleService-Info.plist

**Verification command:**
```bash
unzip -l ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa | grep -E "\.png|\.storyboard|GoogleService"
```

**Status:** ⚠️ IPA needs rebuild to include new assets (icons + launch screen added after build)

### Rebuild Command

```bash
cd /Volumes/Expansion/src/sigilauth/app
make ios-upload
```

**Expected output:** New IPA at `~/Desktop/SigilAuth_AppStore/SigilAuth.ipa` with all resources

---

## Android AAB Verification

### AAB Location

```
android/app/build/outputs/bundle/release/app-release.aab
```

**Status:** ❌ NOT BUILT (Android SDK not installed)

### Verification Commands

Once AAB built:

```bash
# Package info
aapt2 dump badging android/app/build/outputs/bundle/release/app-release.aab | grep -E "package:|versionCode:|versionName:|sdkVersion"

# Permissions
aapt2 dump badging android/app/build/outputs/bundle/release/app-release.aab | grep uses-permission

# Supported screens
aapt2 dump badging android/app/build/outputs/bundle/release/app-release.aab | grep supports-screens

# Architectures
aapt2 dump badging android/app/build/outputs/bundle/release/app-release.aab | grep native-code
```

### Required Resources Checklist

**Launcher Icons:**
- [x] mipmap-mdpi/ic_launcher.png (48x48)
- [x] mipmap-hdpi/ic_launcher.png (72x72)
- [x] mipmap-xhdpi/ic_launcher.png (96x96)
- [x] mipmap-xxhdpi/ic_launcher.png (144x144)
- [x] mipmap-xxxhdpi/ic_launcher.png (192x192)
- [x] Round variants at all densities

**Splash Screen:**
- [x] drawable/ic_splash_logo.png
- [x] values/themes.xml (splash theme)
- [x] values/colors.xml (brand colors)

**Firebase:**
- [x] google-services.json

**Expected AAB Contents:**
- Package ID: `com.wagmilabs.sigil`
- Version code: 1
- Version name: 0.1.0
- Min SDK: 31 (Android 12)
- Target SDK: 34 (Android 14)
- Permissions: INTERNET, CAMERA, USE_BIOMETRIC, FCM
- Native code: arm64-v8a, armeabi-v7a
- Supported screens: all densities

---

## Missing Assets

**iOS:**
- ❌ Screenshots (pending generation — see ios/fastlane/screenshots/README.md)
- ✅ App icons (added 2026-04-26 12:00)
- ✅ Launch screen (added 2026-04-26 13:50)
- ✅ Firebase config

**Android:**
- ❌ Screenshots (pending generation — see android/fastlane/metadata/android/en-US/images/README.md)
- ✅ Launcher icons (added 2026-04-26 12:00)
- ✅ Splash screen (added 2026-04-26 13:50)
- ✅ Firebase config

---

## Locales

**Currently supported:** en-US only

**Strings location:**
- iOS: TBD (awaiting shared-i18n integration)
- Android: TBD (awaiting shared-i18n integration)

**Future:** 47 locales per shared-i18n/.ftl files

---

## Post-Rebuild Verification

After iOS rebuild:

```bash
# Verify new IPA size (should be larger with new assets)
ls -lh ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa

# Check for LaunchScreen
unzip -l ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa | grep LaunchScreen

# Check for app icons
unzip -l ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa | grep AppIcon | wc -l
# Expected: 12 PNG files + Contents.json

# Check for launch logo
unzip -l ~/Desktop/SigilAuth_AppStore/SigilAuth.ipa | grep LaunchLogo | wc -l
# Expected: 3 PNG files + Contents.json
```

After Android build:

```bash
# Verify AAB created
ls -lh android/app/build/outputs/bundle/release/app-release.aab

# Check package ID
aapt2 dump badging android/app/build/outputs/bundle/release/app-release.aab | grep package:

# Expected output:
# package: name='com.wagmilabs.sigil' versionCode='1' versionName='0.1.0'
```

---

**Action Required:**
1. iOS: Rebuild IPA to include new assets (`make ios-upload`)
2. Android: Install SDK, then build AAB (`make android-bundle`)
3. Verify assets present in both bundles using commands above

**Last Updated:** 2026-04-26
