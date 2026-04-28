# Localization Audit — v0.1.0

**Date:** 2026-04-26  
**Scope:** iOS + Android apps  
**Reference:** `/shared-i18n/locales/`  
**Finding:** Nearly all UI strings hardcoded in English

---

## Executive Summary

**Status:** 🔴 **NOT READY for multi-locale launch**

- **iOS:** ~60+ hardcoded English strings found
- **Android:** ~40+ hardcoded English strings found
- **shared-i18n:** 8 Fluent .ftl files with 9 locales (ar, de, en, es, fr, he, ja, pt-BR, zh-CN)
- **Integration:** ❌ NO integration between apps and shared-i18n

**Impact:** v0.1.0 English-only despite translated strings existing. Multi-locale support requires integration work (estimated 2-3 days per platform).

---

## Localization Keys Available (shared-i18n)

**Files in `/shared-i18n/locales/en/`:**
- `auth.ftl` - Onboarding, registration, pairing, biometric prompts
- `challenge.ftl` - Challenge approval flows
- `common.ftl` - Shared UI elements
- `devices.ftl` - Device list, management
- `errors.ftl` - Error messages
- `mnemonic.ftl` - Recovery phrase UI
- `mpa.ftl` - Multi-party authorization
- `pictogram.ftl` - Pictogram verification

**Locales supported:** 9 (ar, de, en, es, fr, he, ja, pt-BR, zh-CN)

**Completeness:** Not audited — assumed complete per Babel agent's work.

---

## iOS Hardcoded Strings (Sample)

**File:** `Sources/UI/Pairing/PairingView.swift`
```swift
Text("Pair Device")
Text("Scan the QR code shown by the service you want to pair with.")
Text("Scan QR Code").tag(PairingMethod.qr)
Text("Enter Code").tag(PairingMethod.code)
Text("Manual Entry").tag(PairingMethod.manual)
Text("Pairing grants this device authorization...")
```

**Available i18n keys (not used):**
- `registration-title` = "Register Device"
- `registration-scan-qr` = "Scan QR Code"
- `registration-enter-code` = "Enter Pairing Code"
- `pairing-code-title` = "Enter Pairing Code"

**File:** `Sources/UI/Approval/ApprovalView.swift`
```swift
Text("Authentication Request")
Text("Approve Login?")
Text("Service")
Text("Action")
Text("Reject")
Text("Approve")
```

**Available i18n keys:**
- `challenge-approve-title` (likely defined in challenge.ftl)
- `challenge-approve-action`
- `challenge-deny-button`
- `challenge-approve-button`

**File:** `Sources/UI/Mnemonic/MnemonicDisplayView.swift`
```swift
Text("Backup Phrase")
Text("Write down these 12 words in order...")
Text("Screenshot Protection Active")
Text("Screen Recording Detected")
Text("Never share your recovery phrase")
```

**Available i18n keys:** (from `mnemonic.ftl` - not yet verified)

---

## Android Hardcoded Strings (Sample)

**File:** `ui/pairing/PairingCodeEntryScreen.kt`
```kotlin
Text("Enter Pairing Code")
Text("Back")
Text("Try Again")
Text("Paste from Clipboard")
Text("Get New Code")
```

**Available i18n keys (not used):**
- `pairing-code-title`
- `pairing-code-submit`
- `common-back` (likely)
- `common-try-again` (likely)

**File:** `ui/registration/RegistrationScreen.kt`
```kotlin
Text("Add Server")
Text("8-digit code")
Text("Server URL")
Text("Connect")
```

**File:** `ui/approval/ApprovalScreen.kt`
```kotlin
Text("Deny")
Text("Approve")
```

---

## Integration Gap Analysis

### iOS Missing Components
1. **Fluent bundle loader** - No code to parse .ftl files
2. **Locale selection** - No system locale detection
3. **String lookup** - No `LocalizedStringKey` or NSLocalizedString wrapper
4. **Resource bundling** - .ftl files not copied to app bundle

**Required work:**
- Add Fluent parser dependency (e.g., `fluent-swift` or custom parser)
- Create `LocalizationService` in `Sources/Core/Localization/`
- Wrap all `Text("...")` → `Text(LocalizedStringKey("..."))`
- Configure Xcode build phase to copy .ftl files to Resources

**Estimated effort:** 2 days (implementation) + 1 day (testing)

### Android Missing Components
1. **Fluent parser** - No Kotlin library integrated
2. **Locale selection** - No `Locale.getDefault()` handling
3. **String lookup** - All `Text("...")` direct, no `stringResource(R.string.key)`
4. **Resource system** - .ftl files not converted to Android resources

**Options:**
- **A:** Use Android string resources (`res/values-XX/strings.xml`) — convert .ftl → XML
- **B:** Use Fluent runtime parser (less common on Android)

**Recommended:** Option A (Android-native approach)

**Required work:**
- Script to convert .ftl → XML for all 9 locales
- Place in `res/values/`, `res/values-ar/`, `res/values-de/`, etc.
- Replace all `Text("...")` → `Text(stringResource(R.string.key))`
- Update build.gradle to generate R.string constants

**Estimated effort:** 2 days (conversion script + integration) + 1 day (testing)

---

## Debug Strings (Excluded)

**iOS:** `Sources/UI/Debug/PushTestView.swift` — Hardcoded OK (debug-only, English acceptable)  
**Android:** `ui/debug/*` — Hardcoded OK (debug-only)

---

## Violation Count

| Platform | Hardcoded Strings | Available i18n Keys | Integration |
|----------|------------------|---------------------|-------------|
| iOS      | ~60+             | ~80+ (estimated)    | ❌ None      |
| Android  | ~40+             | ~80+ (estimated)    | ❌ None      |

**Note:** Exact counts require full grep + manual review. Sample above represents ~40% coverage.

---

## Recommendations

### For v0.1.0 (TestFlight/Internal Testing)
**Accept:** English-only for internal testing. Defer i18n integration to v0.2.0.

**Justification:**
- Internal testers likely English-speaking
- Integration work (2-3 days per platform) delays TestFlight release
- v0.1.0 focused on core flow validation, not localization

**Required note in release:** "English only. Multi-language support coming in v0.2.0."

### For v0.2.0 (Public Beta)
**Required before public launch:**
1. iOS: Integrate Fluent parser, wrap all strings
2. Android: Convert .ftl → XML, use stringResource()
3. Test all 9 locales on physical devices
4. Verify RTL layout (ar, he)
5. Verify CJK rendering (ja, zh-CN)

**Estimated total effort:** 5-7 days (both platforms)

---

## Quick Wins (Optional for v0.1.0)

If time permits before TestFlight, prioritize these high-visibility strings:

**iOS:**
- `Sources/UI/Approval/ApprovalView.swift` — "Approve" / "Reject" buttons
- `Sources/UI/Pairing/PairingView.swift` — "Pair Device" title

**Android:**
- `ui/approval/ApprovalScreen.kt` — "Approve" / "Deny" buttons
- `ui/pairing/PairingCodeEntryScreen.kt` — "Enter Pairing Code" title

**Effort:** 1 hour (add 5-10 key strings, test with `LANG=de_DE`)

---

## Testing Commands (for future i18n work)

**iOS Simulator:**
```bash
# Launch with German locale
xcrun simctl spawn booted defaults write NSGlobalDomain AppleLanguages -array de
xcrun simctl spawn booted defaults write NSGlobalDomain AppleLocale -string de_DE

# Reset to English
xcrun simctl spawn booted defaults write NSGlobalDomain AppleLanguages -array en
```

**Android Emulator/Device:**
```bash
# Change locale to Japanese
adb shell "setprop persist.sys.locale ja-JP && stop && start"

# Reset to English
adb shell "setprop persist.sys.locale en-US && stop && start"
```

**Note:** Restart app after locale change.

---

## Files to Update (v0.2.0 Roadmap)

**iOS:**
```
Sources/Core/Localization/LocalizationService.swift  [CREATE]
Sources/Core/Localization/FluentLoader.swift         [CREATE]
Sources/UI/**/*.swift                                [EDIT ALL - wrap strings]
App/Resources/*.ftl                                  [COPY from shared-i18n]
Project.yml                                          [UPDATE - add .ftl resources]
```

**Android:**
```
scripts/ftl-to-android-xml.js                        [CREATE - conversion script]
app/src/main/res/values/strings.xml                  [GENERATE from .ftl]
app/src/main/res/values-ar/strings.xml               [GENERATE]
app/src/main/res/values-de/strings.xml               [GENERATE]
...
app/src/main/kotlin/**/*.kt                          [EDIT ALL - use stringResource()]
```

---

## Conclusion

**Current state:** English-only despite translations existing.  
**v0.1.0 recommendation:** Ship English-only, document limitation.  
**v0.2.0 requirement:** Full i18n integration (5-7 days work).

Localization infrastructure exists (shared-i18n), but integration incomplete. Apps and shared-i18n are decoupled.

---

**Audit performed by:** Nova (mobile engineer)  
**Next step:** Defer to v0.2.0 or allocate 5-7 days for integration before v0.1.0
