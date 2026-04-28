# Changelog

All notable changes to the Sigil Auth Android app will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-26

### Added
- **Device registration flow** — Register device with Sigil Auth servers via QR code scanner, 8-digit code, or manual URL
- **Hardware-backed cryptography** — ECDSA P-256 keypairs in Android Keystore with StrongBox (when available), never exported
- **Push notification approval** — Receive authentication requests via FCM, approve with fingerprint/face unlock
- **Challenge-response authentication** — Cryptographic signature verification for every login
- **QR code scanner** — CameraX + ML Kit barcode detection for instant pairing
- **Approval screen** — Shows server name, action, metadata before biometric prompt
- **Splash screen** — Sigil logo with Android 12+ splash screen API
- **Launcher icons** — All densities (mdpi through xxxhdpi, round variants)
- **Firebase Crashlytics** — Automatic crash reporting with debug test menu
- **Accessibility support** — TalkBack descriptions, font scaling, 48dp touch targets, WCAG 2.2 AA color contrast
- **Material Design 3** — Dynamic color, elevated cards, modern typography
- **Localization ready** — String resources structure for 47-locale support (English only in v0.1.0)

### Technical Details
- **Minimum Android version:** 12 (API 31) — StrongBox availability, modern splash API
- **Target Android version:** 14 (API 34)
- **Kotlin version:** 1.9.22
- **Architecture:** Jetpack Compose + ViewModel, MVI pattern
- **Crypto:** Android Keystore (StrongBox when available), ECDSA P-256, AES-256-GCM
- **Push:** Firebase Cloud Messaging with relay.sigilauth.com
- **Package:** com.wagmilabs.sigil
- **Developer:** Wagmi Labs Pty Ltd

### Known Limitations
- **No mnemonic backup** — Feature not implemented on Android (iOS has it)
- **No pictogram verification** — Feature not implemented on Android (iOS has it)
- **No device list** — Can't view all paired servers
- **No unpair flow** — Must uninstall app to unpair
- **No settings screen** — No way to view device fingerprint after registration
- **No offline queue** — Challenge responses fail without network
- **MPA/Decrypt flows not implemented** — Waiting for backend Phase C + mobile Phase D

### Security Notes
- Private keys never leave Android Keystore
- StrongBox backend used when device supports it (hardware TEE)
- All network communication over TLS 1.3
- ProGuard enabled in release builds (code obfuscation)
- Firebase services: FCM + Crashlytics only (no Analytics)

### Accessibility Compliance
- WCAG 2.2 Level AA compliant
- TalkBack: All interactive elements have content descriptions
- Font scaling: Uses Material Typography (scales with system font size)
- Touch targets: All ≥48dp minimum
- Color contrast: 4.5:1 minimum for normal text, 3:1 for large text/UI components
- Audit report: `working/mobile/accessibility-audit.md`

### Distribution
- **Play Console:** Internal testing only (v0.1.0)
- **Play Store:** Not submitted (awaiting production readiness)

---

## Release Notes Format

For Play Console release notes (500 char limit), see:
- `store-listing/release-notes-v0.1.0.txt`

---

**License:** AGPL-3.0  
**Source:** https://github.com/sigilauth/app  
**Support:** support@sigilauth.com
