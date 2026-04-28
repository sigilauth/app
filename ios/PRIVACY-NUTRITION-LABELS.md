# App Store Privacy Nutrition Labels

**App:** Sigil Auth  
**Bundle ID:** com.wagmilabs.sigil  
**Date:** 2026-04-26  
**Prepared by:** Nova (Mobile Engineer)

---

## Summary

**Data Used to Track You:** None  
**Data Linked to You:** Identifiers (Device ID only)  
**Data Not Linked to You:** None by default (Diagnostics if crash reporting enabled)

---

## Detailed Questionnaire Responses

### 1. Do you or your third-party partners collect data from this app?

**YES** — We collect device identifiers (fingerprint derived from public key).

---

### 2. Data Used to Track You

**Definition:** Data used to track users across apps/websites owned by other companies for advertising or to share with data brokers.

**Sigil's Answer:** **NONE**

**Justification:**
- Sigil Auth does not track users
- No advertising SDKs
- No analytics that track across apps
- Device fingerprint used only for cryptographic identity, never shared with third parties
- Open source AGPL-3.0 — anyone can verify

**Source code references:**
- `ios/Sources/Core/CryptoService.swift` — keypair generation, fingerprint derivation (local only)
- No Firebase Analytics, no Meta SDK, no Google Analytics
- Push notifications via APNs only (Apple's service, not third-party tracking)

---

### 3. Data Linked to You

**Definition:** Data connected to the user's identity (name, account, device).

#### Identifiers

**Collected:** YES  
**Type:** Device ID (cryptographic fingerprint)  
**Purpose:** App Functionality  
**Source:** Derived from device public key (ECDSA P-256)

**Details:**
- **What:** SHA-256 hash of device ECDSA public key
- **Why:** Cryptographic identity for authentication challenges
- **Where stored:** 
  - On device: Keychain (private key) + UserDefaults (fingerprint hash)
  - On paired servers: Server database (public key + fingerprint for signature verification)
- **Transmitted:** YES — sent to paired servers during registration, included in signed challenge responses
- **Deletable:** YES — user can unpair device, server deletes fingerprint
- **Source code:** `ios/Sources/Core/KeychainService.swift:generateKeypair()`, `ios/Sources/Core/CryptoService.swift:deriveFingerprint()`

**NOT Collected:**
- Name, Email, Phone, User ID (beyond fingerprint)
- Advertising identifier (IDFA)
- Device identifiers for other purposes

---

### 4. Data Not Linked to You

**Definition:** Data collected but not linked to user identity.

**Sigil's Answer:** **NONE by default**

**If crash reporting enabled (opt-in, future feature):**
- **Diagnostics → Crash Data:** Stack traces, device model, OS version
- **Purpose:** App performance improvement
- **Not linked to identity:** No fingerprint in crash reports
- **Current status:** Not yet implemented

---

### 5. Categories We Do NOT Collect

Mark all as **NO**:

- **Contact Info** — No name, email, phone, address
- **Health & Fitness** — No health data
- **Financial Info** — No payment info, credit cards, purchase history
- **Location** — No location data
- **Sensitive Info** — No racial/ethnic data, sexual orientation, pregnancy, disability, political affiliation
- **Contacts** — No access to contacts
- **User Content** — No photos, videos, audio, emails, messages
- **Browsing History** — No web browsing, search history
- **Search History** — No search data
- **Purchases** — No in-app purchases, purchase history
- **Usage Data** — No product interaction, advertising data, usage tracking
- **Other Diagnostic Data** — None (unless crash reporting opt-in)

**Biometric Data:** 
- **Collected by Sigil Auth:** NO
- **Explanation:** Face ID/Touch ID used on-device only via LocalAuthentication framework. Biometric data never leaves Secure Enclave, never transmitted, never stored by Sigil Auth. Apple handles all biometric processing.
- **Source:** iOS LocalAuthentication API documentation — biometric data stays in Secure Enclave

---

## Privacy Practices Summary

### Data Collection: Minimal

**Only collected:**
1. Device fingerprint (SHA-256 of public key) — required for cryptographic authentication

**Purpose:**
- App Functionality — identity verification, challenge signing

### Data Usage: Transparent

- Device fingerprint sent to servers user explicitly pairs with
- No third-party sharing
- No advertising
- No analytics that track users
- Open source — all data collection visible in code

### Data Protection: Hardware-Backed

- Private key never leaves Secure Enclave
- Fingerprint derived on-device
- Mnemonic encrypted in Keychain (never transmitted)
- No cloud backup of keys (user must write mnemonic)

### User Control

- User chooses which servers to pair with
- User can unpair at any time (deletes data from server)
- User can delete app (deletes all local data)
- No account creation — device is identity

---

## App Store Connect Entry

**Privacy Policy URL:** https://sigilauth.com/privacy

**Questionnaire Answers:**

1. Does this app collect data?  
   → **Yes**

2. Data used to track you?  
   → **No**

3. Data linked to you?  
   → **Yes** → Identifiers → Device ID → App Functionality

4. Data not linked to you?  
   → **No** (unless crash reporting enabled in future)

5. All other categories?  
   → **No**

---

## Verification

**How to verify our claims:**

1. Clone repo: `git clone https://github.com/sigilauth/app`
2. Search for data collection:
   ```bash
   grep -r "URLSession\|Alamofire\|Analytics\|Firebase" ios/
   # Result: Only URLSession for challenge/response, no analytics
   ```
3. Check third-party dependencies: `ios/Package.swift`
   - Result: Zero third-party SDKs
4. Review protocol spec: `api/openapi.yaml`
   - Only endpoint: `/register` (send public key), `/challenge` (receive), `/response` (send signed)

**Open source license:** AGPL-3.0 — all code auditable at github.com/sigilauth/app

---

**Last Updated:** 2026-04-26  
**Contact:** privacy@sigilauth.com (or support@sigilauth.com)
