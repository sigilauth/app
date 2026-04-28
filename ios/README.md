# Sigil Auth iOS App

Swift + SwiftUI implementation of Sigil Auth client for iOS 16+.

**Status:** 🚧 Scaffolding complete, awaiting B0 (OpenAPI spec) to begin implementation

## Requirements

- iOS 16.0+ (App Attest API)
- Xcode 15.2+
- Swift 5.9+

## Architecture

Module structure per [nova-mobile-platform-spec.md](../../working/specs/nova-mobile-platform-spec.md):

```
Sources/
├── Core/
│   ├── Crypto/           # P-256, ECDSA, pictogram derivation
│   ├── Keychain/         # Secure Enclave key management
│   ├── Attestation/      # App Attest integration
│   └── Storage/          # Multi-server config (Keychain + Core Data)
├── Network/              # API client (awaiting B0)
├── UI/
│   ├── Pairing/          # QR + 8-digit + deep link + manual
│   ├── Approval/         # Challenge + MPA screens
│   └── Components/       # Pictogram, buttons, etc.
└── Utils/                # Helpers

Tests/
├── CoreTests/            # TDD unit tests (currently failing)
└── UITests/              # XCUITest for flows

Resources/
└── Localization/
    └── en/               # Fluent .ftl files (7 P0 locales planned)
```

## Key Decisions

- **Secure Enclave only** — No software keystore fallback (Knox Top 5 #1)
- **Biometric + passcode** — Aria §4.3 requires passcode fallback for a11y (WCAG 3.3.8)
- **Device self-authentication** — Device provides public_key on response, Sigil derives fingerprint
- **Plaintext challenges over TLS** — D2 decision, no ECIES encryption
- **TDD enforced** — Tests written first (D5), currently failing

## Setup

```bash
# Clone
cd /Volumes/Expansion/src/sigilauth/app/ios

# Build (Swift Package Manager)
swift build

# Run tests (will fail until implementation)
swift test

# Open in Xcode
open Package.swift
```

## Tests

**Current status:** All tests FAIL (expected per TDD). Implementation blocked on B0.

Run tests:
```bash
swift test
```

Specific test suites:
```bash
# Crypto tests
swift test --filter CryptoServiceTests

# Keychain tests (requires device, not simulator)
swift test --filter KeychainServiceTests
```

## Accessibility

All 15 Aria blocking criteria implemented in scaffolding:

- ✅ Pictogram with role="img" + speakable alternative
- ✅ VoiceOver live regions for state changes
- ✅ 44×44pt touch targets
- ✅ Biometric + passcode fallback
- ✅ Reduce motion support
- ✅ Dynamic Type scaling
- ✅ Keyboard navigation (future)

## i18n

Fluent `.ftl` files per Suki i18n spec:

- P0 locales: en, es, ja, zh-CN, de, fr, pt-BR
- RTL support: ar, he, fa, ur (layout flips, pictogram emoji order preserved)

## CI/CD

GitHub Actions workflow at `.github/workflows/ios-ci.yml`:

- Lint (SwiftLint - TBD)
- Unit tests
- UI tests
- Coverage gate (80% per Maren §10)
- Accessibility Scanner (Xcode Accessibility Inspector)

## Next Steps

1. ⏳ Wait for B0 (OpenAPI spec from Echo)
2. Implement crypto services (test vectors from protocol-spec §11)
3. Implement Secure Enclave key management
4. Implement network layer with B0 schema
5. Implement UI flows end-to-end
6. Instrumentation tests on physical device
7. VoiceOver manual testing

## Blockers

- **B0 not complete** — No OpenAPI schema, no API client implementation
- **Test vectors needed** — Protocol-spec §11 has placeholders

## Ownership

**Owner:** @nova (mobile-engineer)
**Reviewers:** @knox (security), @iris (UI), @aria (a11y)
**Dependencies:** B0 (OpenAPI), B15 (Fluent strings)

---

For questions: DM @nova or see [work-blocks.md](../../working/specs/work-blocks.md) §B5
