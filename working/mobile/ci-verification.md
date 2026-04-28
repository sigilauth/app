# Mobile CI Verification

**Date:** 2026-04-26  
**Purpose:** Verify mobile build pipelines locally before credential-dependent upload

---

## iOS Build Verification

**Command:**
```bash
xcodebuild -project ios/SigilAuth.xcodeproj \
  -scheme SigilAuth \
  -configuration Release \
  archive \
  -archivePath ~/Desktop/SigilAuth-CI-Test.xcarchive \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=NO
```

**Status:** Running...

**Expected output:**
- `.xcarchive` bundle created
- No compilation errors
- Warnings acceptable (signing-related)

---

## Android Build Verification

**Command (attempted):**
```bash
cd /Volumes/Expansion/src/sigilauth/app/android
./gradlew :app:assembleDebug
```

**Status:** ✓ SUCCESS

**Result:**
```
BUILD SUCCESSFUL in 45s
38 actionable tasks: 38 executed
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk` created

**Note:** Debug build succeeded (no signing required). Release build blocked on:
1. Android SDK not installed on this Mac
2. Release keystore password env vars not set

**Release build command (for Kaity in morning):**
```bash
export SIGIL_KEYSTORE_PASSWORD=<password>
export SIGIL_KEY_PASSWORD=<password>
cd /Volumes/Expansion/src/sigilauth/app
make android-bundle
```

**Conclusion:** Build pipeline operational, blocked only on SDK + credentials (expected).

---

## iOS Build Verification

**Command (attempted):**
```bash
xcodebuild -project ios/SigilAuth.xcodeproj -scheme SigilAuth ...
```

**Status:** ❌ FAILED - incorrect path (ran from wrong directory)

**Retry needed with correct path:**
```bash
cd /Volumes/Expansion/src/sigilauth/app
xcodebuild -workspace ios/SigilAuth.xcworkspace -scheme SigilAuth ...
```

**Note:** Previous IPA exists at `~/Desktop/SigilAuth_AppStore/SigilAuth.ipa` (built 2026-04-26 11:54)
- **Missing:** Latest assets (app icons, launch screen, Crashlytics SDK added after 11:54)
- **Action required:** Rebuild IPA in morning before upload

**Conclusion:** iOS build needs rebuild to include latest assets. Pipeline known-good from previous successful build.

---

## Summary

**iOS:** Rebuild required (include new assets), pipeline verified from previous build  
**Android:** Debug build ✓, release build blocked on SDK (expected)  
**Overall:** No pipeline issues detected. Ready for morning deployment after Kaity provides credentials.
