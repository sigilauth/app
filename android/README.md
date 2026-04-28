# Sigil Auth Android App

**Status:** 🚧 Scaffolding complete — Awaiting B0 (OpenAPI spec)

Native Android app for Sigil Auth MVP. Hardware-backed PKI authentication with StrongBox, FCM push, and biometric gating.

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── kotlin/com/sigilauth/app/
│   │   │   ├── SigilAuthApplication.kt      # Application class
│   │   │   ├── MainActivity.kt              # Main entry point
│   │   │   ├── core/
│   │   │   │   ├── crypto/
│   │   │   │   │   ├── KeystoreManager.kt  # StrongBox keypair generation
│   │   │   │   │   └── CryptoUtils.kt      # P-256 compression, fingerprint derivation
│   │   │   │   └── biometric/
│   │   │   │       └── BiometricAuthenticator.kt # Biometric + device credential
│   │   │   ├── services/
│   │   │   │   └── SigilMessagingService.kt # FCM push handling
│   │   │   └── ui/
│   │   │       └── theme/                   # Material3 theme (Iris design tokens)
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── values-night/
│   │           └── themes.xml               # Dark mode
│   └── test/
│       └── kotlin/com/sigilauth/app/
│           └── core/crypto/
│               └── CryptoUtilsTest.kt       # TDD unit tests
├── build.gradle.kts                          # App-level Gradle config
└── proguard-rules.pro                        # ProGuard/R8 rules
```

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 1.9.22 |
| UI Framework | Jetpack Compose | 1.6.0 |
| Min SDK | 31 (Android 12) | StrongBox availability |
| Target SDK | 34 (Android 14) | Latest |
| Security | AndroidX Security, BouncyCastle | 1.1.0-alpha06, 1.77 |
| Database | Room | 2.6.1 |
| Network | Retrofit, OkHttp | 2.9.0, 4.12.0 |
| Push | Firebase Cloud Messaging | 32.7.1 |
| QR Scanning | CameraX, ML Kit | 1.3.1, 17.2.0 |
| Testing | JUnit, MockK, Robolectric | 4.13.2, 1.13.9, 4.11.1 |

## Build & Run

**Prerequisites:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

**Build:**
```bash
./gradlew assembleDebug
```

**Run tests:**
```bash
./gradlew test                 # Unit tests
./gradlew connectedCheck       # Instrumented tests (requires device/emulator)
```

**Install to device:**
```bash
./gradlew installDebug
```

## Scaffolding Status

✅ **Complete:**
- Gradle multi-module setup with Kotlin DSL
- Jetpack Compose + Material3 theming (Iris design tokens)
- StrongBox keystore manager (detection + TEE fallback)
- Biometric authenticator (BIOMETRIC_STRONG + device credential fallback per Aria §4.3)
- P-256 key compression utilities
- FCM service skeleton
- Deep link handling (sigil:// + App Links)
- TDD test infrastructure (JUnit5, MockK, Robolectric)
- ProGuard/R8 configuration for release builds
- RTL support enabled (`android:supportsRtl="true"`)

🚧 **Awaiting B0 (OpenAPI spec):**
- Network layer (Retrofit API client)
- Challenge/response data models
- Registration flow implementation
- Approval screen UI
- MPA flow implementation
- Mnemonic generation UI
- Room database schema
- i18n Fluent integration
- Full test coverage (target: 80%)

## Security Features

Per knox-threat-model.md Top 5:

1. **Hardware key extraction infeasible** — Private keys in StrongBox/TEE, non-exportable
2. **Biometric gate on every signing** — `setUserAuthenticationValidityDurationSeconds(-1)`
3. **Device self-authentication** — Fingerprint derived from public key, no device DB
4. **Plaintext challenges over TLS** — No ECIES for challenge wire (D2)
5. **Stateless server** — All state in integrator DB or relay

## Accessibility

Per aria-a11y-requirements.md:

- ✅ Device credential fallback (WCAG 3.3.8 AA)
- ✅ RTL support (ar, he, fa, ur)
- ⏸️ TalkBack accessible pictogram pattern (pending UI implementation)
- ⏸️ Touch targets ≥44×44dp (pending UI implementation)
- ⏸️ Color contrast 4.5:1+ (Material3 theme configured, pending verification)

## Coordination

**Blocking on:**
- @kai: B0 (OpenAPI spec) for API client implementation
- @kai-relay: FCM token registration contract

**In progress:**
- @suki: Fluent Android binding (fluent-rs JNI or Java port)

**Future coordination:**
- @iris: Design system components (buttons, cards, pictogram display)
- @knox: Key attestation X.509 parsing approach
- @nova (iOS): Shared mobile patterns

## License

AGPL-3.0 — See `/LICENSE`

---

**Nova (Android) — 2026-04-23**
