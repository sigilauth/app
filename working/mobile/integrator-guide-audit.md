# Integrator Mobile-First Guide Audit

**Date:** 2026-04-26  
**Document:** `/docs/integrator-mobile-first.md`  
**Audited against:** Current iOS (v0.1.0) + Android (v0.1.0) implementations

---

## Executive Summary

Overall guide is **structurally sound** with accurate integration patterns. Found **3 critical inaccuracies** requiring updates and **2 missing sections** that would improve adoption.

---

## ✅ Accurate Sections

### Path 1: Official Sigil Auth App
- ✓ URL scheme `sigil://` correct (verified in `ios/Project.yml`)
- ✓ Deep link pattern matches current implementation
- ✓ Backend SDK pseudocode aligns with server API (not yet implemented but matches spec)
- ✓ Push notification flow accurately described

### Path 2: Embedded SDK - iOS
- ✓ `CryptoService` exists in `Sources/Core/Crypto/CryptoService.swift`
- ✓ `KeychainService` exists in `Sources/Core/Keychain/KeychainService.swift`
- ✓ Swift Package structure matches (`SigilAuthCore`, `SigilAuthUI` products)
- ✓ Biometric flow with `LAContext` matches current usage

### Common Pitfalls
- ✓ Push notification setup steps accurate
- ✓ Deep link testing commands (`xcrun simctl openurl`, `adb shell`) valid
- ✓ Biometric prompt timing guidance matches current UX (show context first, then prompt)
- ✓ Mnemonic backup exclusion code accurate

---

## ❌ Critical Inaccuracies (Must Fix)

### 1. Android Package Name Mismatch

**Current in guide:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.sigilauth:sigilauth-android:0.1.0")
}

import com.sigilauth.core.CryptoManager
import com.sigilauth.core.KeystoreManager
```

**Actual implementation:**
```kotlin
package com.wagmilabs.sigil.core.crypto

// Files:
- KeystoreManager.kt
- SigningManager.kt
- CryptoUtils.kt
// (No CryptoManager — use SigningManager instead)
```

**Fix required:**
- Update package name from `com.sigilauth.*` → `com.wagmilabs.sigil.*`
- Update class name `CryptoManager` → `SigningManager`
- Note: v0.1.0 is NOT published to Maven Central (internal build only)

### 2. Repository URL Assumption

**Current in guide:**
```swift
.package(url: "https://github.com/sigilauth/app", from: "0.1.0")
```

**Status:** Repository not confirmed public at this URL.  
**Fix:** Verify actual GitHub org (`sigilauth` vs `wagmilabs`) before publish, or note "Coming soon" if not yet available.

### 3. Backend SDK Availability

Guide shows `@sigilauth/sdk` (npm) and `github.com/sigilauth/server/sdk-go`.

**Status:** Server implementation in progress (Phase B). SDKs not published.  
**Fix:** Add note:
> ⚠️ **Note:** Server SDKs unreleased. Example code shows target API. Track progress at [sigilauth/server](https://github.com/sigilauth/server).

---

## ⚠️ Missing Sections (Should Add)

### 1. GoogleService-Info.plist / google-services.json Setup

Guide mentions Firebase dependency but doesn't explain:
- Where to get `google-services.json` (Firebase Console)
- Where to place it (`android/app/google-services.json`)
- iOS equivalent: `GoogleService-Info.plist` (not mentioned at all)

**Recommendation:** Add subsection under "Path 2: Embedded SDK" → "Push Notification Configuration" with:
- Firebase project creation steps
- Download + placement instructions
- Screenshot of Firebase Console

### 2. Testing Without Backend

Integrators building mobile-first hit blocker: no backend yet.

**Recommendation:** Add section "Testing Offline Mode":
```swift
// iOS: Stub for local testing
let mockChallenge = Challenge(
    id: UUID(),
    serverName: "test.local",
    action: "login",
    metadata: [:],
    expiresAt: Date().addingTimeInterval(300)
)
```

Helps developers validate biometric + signing flow before backend ready.

---

## 🔍 Minor Issues (Low Priority)

### 1. cli-device Section

Path 3 references `cli-device` for backend testing. This tool exists at `/cli-device/` (confirmed in working directory) but:
- Installation path unclear (npm package? local build?)
- Commands shown assume it's in PATH

**Fix:** Add installation command:
```bash
# Install globally
npm install -g @sigilauth/cli-device

# Or run from source
cd cli-device && npm install && npm link
```

### 2. Pictogram Verification Not Emphasized

Guide mentions pictogram once in "Common Pitfalls" but doesn't emphasize mutual authentication security benefit in Path 1/2 integration steps.

**Recommendation:** Add callout box in Path 2:
> 🔐 **Security Note:** Always display the server's pictogram before biometric prompt. Users verify they're authenticating to the correct server, not a phishing attempt.

---

## 📊 Audit Metrics

| Category | Count | Status |
|----------|-------|--------|
| Accurate code examples | 12 | ✅ |
| Inaccurate references | 3 | ❌ Fix required |
| Missing sections | 2 | ⚠️ Should add |
| Minor clarifications | 2 | 💡 Nice-to-have |

**Overall Grade:** B+ (fixes required before public launch)

---

## Recommended Action Plan

**Before v0.1.0 public launch:**
1. Fix Android package names (5 min)
2. Verify GitHub repo URLs or add "unreleased" notes (2 min)
3. Add Firebase config section with screenshots (20 min)

**Before v0.2.0:**
1. Add offline testing stubs section (10 min)
2. Enhance pictogram security messaging (5 min)
3. Clarify cli-device installation (5 min)

**Total fix time:** ~30 minutes for critical path (items 1-3 above)

---

## Files to Update

```
docs/integrator-mobile-first.md
  - Line ~278: Android package name
  - Line ~285: Class name CryptoManager → SigningManager
  - Line ~214: Add note about SDK availability
  - Add new section after line ~360: Firebase setup
```

---

**Audit performed by:** Nova (mobile engineer)  
**Next step:** Apply fixes or hand off to @cora if copy changes needed
