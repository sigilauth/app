# Changelog

All notable changes to the Sigil Auth iOS app will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-26

### Added
- **Device pairing flow** — Register device with Sigil Auth servers via QR code, 8-digit code, or manual URL entry
- **Hardware-backed cryptography** — ECDSA P-256 keypairs stored in Secure Enclave, never exported
- **Push notification approval** — Receive authentication requests via APNs, approve with Face ID/Touch ID
- **Challenge-response authentication** — Cryptographic signature verification for every login
- **Mnemonic backup** — 12-word BIP-39 recovery phrase with screenshot protection
- **Pictogram verification** — Visual server identity verification with 5-emoji pictogram
- **Multi-party authorization (MPA)** — Approve destructive actions with M-of-N device signatures (UI only, server support pending)
- **Decrypt flow** — ECIES-encrypted payload decryption with biometric gate (UI only, server support pending)
- **Accessibility support** — VoiceOver labels, Dynamic Type scaling, 44pt touch targets, WCAG 2.2 AA color contrast
- **Launch screen** — Sigil logo on dark background (#121217)
- **App icons** — All required sizes (@1x/@2x/@3x, 1024px App Store icon)
- **Firebase Crashlytics** — Automatic crash reporting with debug test menu
- **Localization ready** — Fluent.swift integration for 47-locale support (English only in v0.1.0)

### Technical Details
- **Minimum iOS version:** 16.0
- **Swift version:** 5.9
- **Architecture:** SwiftUI + Combine, MVVM pattern
- **Crypto:** CryptoKit (Secure Enclave), ECDSA P-256, AES-256-GCM
- **Push:** APNs with relay.sigilauth.com
- **Bundle ID:** com.wagmilabs.sigil
- **Team:** Wagmi Labs LLC (CVBUNQ5UY3)

### Known Limitations
- **No mnemonic verification** — User can't test recovery before losing device
- **No device list** — Can't view all paired servers
- **No unpair flow** — Must delete app to unpair
- **No settings screen** — No way to view device fingerprint or pictogram after first pairing
- **No offline queue** — Challenge responses fail without network
- **MPA/Decrypt server support pending** — UI built, waiting for backend Phase C

### Security Notes
- Private keys never leave Secure Enclave
- Mnemonic stored encrypted in Keychain, excluded from iCloud backup
- Screenshot protection active on mnemonic display screen
- All network communication over TLS 1.3
- App Attest attestation integrated (production environment)

### Accessibility Compliance
- WCAG 2.2 Level AA compliant
- VoiceOver: All interactive elements labeled, logical focus order
- Dynamic Type: All text scales with system font size
- Touch targets: All ≥44pt minimum
- Color contrast: 4.5:1 minimum for normal text, 3:1 for large text/UI components
- Audit report: `working/mobile/accessibility-audit.md`

### Distribution
- **TestFlight:** Internal testing only (v0.1.0)
- **App Store:** Not submitted (awaiting production readiness)

---

## Release Notes Format

For App Store Connect release notes (4000 char limit), see:
- `store-listing/release-notes-v0.1.0.txt`

---

**License:** AGPL-3.0  
**Source:** https://github.com/sigilauth/app  
**Support:** support@sigilauth.com
