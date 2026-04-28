# Play Console Data Safety Form

**App:** Sigil Auth  
**Package:** com.wagmilabs.sigil  
**Date:** 2026-04-26  
**Prepared by:** Nova (Mobile Engineer)

---

## Summary

**Data collected:** Device ID (cryptographic fingerprint) only  
**Data shared:** None  
**Data encrypted in transit:** Yes (TLS 1.3)  
**User can request data deletion:** Yes (unpair device)  
**Data collection optional:** No (required for core functionality)

---

## Play Console Questionnaire Responses

### Section 1: Does your app collect or share user data?

**YES** — App collects device identifiers for cryptographic authentication.

**Follow-up:** Is all data collection and sharing optional?  
**NO** — Device fingerprint required for app to function.

---

### Section 2: Data Types

Play Console categorizes data into these groups. Mark what applies:

#### Location
- [ ] Approximate location
- [ ] Precise location

**Sigil's Answer:** **NONE** — No location data collected.

---

#### Personal info
- [ ] Name
- [ ] Email address
- [ ] User IDs
- [x] **Device or other IDs**  
  - **Type:** Device fingerprint (SHA-256 of ECDSA P-256 public key)
  - **Collected:** YES
  - **Shared:** NO (sent only to servers user explicitly pairs with)
  - **Ephemeral:** NO (persisted for authentication)
  - **Required:** YES (core functionality)
  - **Purpose:** App functionality — cryptographic identity
- [ ] Address
- [ ] Phone number
- [ ] Race and ethnicity
- [ ] Political or religious beliefs
- [ ] Sexual orientation
- [ ] Other info

**Details:**
- **What we collect:** SHA-256 hash derived from device's ECDSA public key
- **Why:** Identifier for cryptographic challenge-response authentication
- **Where stored:** 
  - Device: Android Keystore (private key) + SharedPreferences (fingerprint hash)
  - Paired servers: Server database (public key for signature verification)
- **User control:** User can unpair device (triggers server deletion)

**Source code:** `android/app/src/main/kotlin/com/sigilauth/app/core/crypto/KeystoreManager.kt`

---

#### Financial info
- [ ] User payment info
- [ ] Purchase history
- [ ] Credit score
- [ ] Other financial info

**Sigil's Answer:** **NONE** — No financial data collected.

---

#### Health and fitness
- [ ] Health info
- [ ] Fitness info

**Sigil's Answer:** **NONE** — No health data collected.

---

#### Messages
- [ ] Emails
- [ ] SMS or MMS
- [ ] Other in-app messages

**Sigil's Answer:** **NONE** — No messages collected.

---

#### Photos and videos
- [ ] Photos
- [ ] Videos

**Sigil's Answer:** **NONE** — Camera used only for QR scanning, images not stored.

**Note:** QR scanner processes images in-memory only (ML Kit barcode detection). No photos saved to disk or transmitted.

**Source:** `android/app/src/main/kotlin/com/sigilauth/app/ui/qr/QRScannerScreen.kt:QRCodeAnalyzer`

---

#### Audio files
- [ ] Voice or sound recordings
- [ ] Music files
- [ ] Other audio files

**Sigil's Answer:** **NONE** — No audio collected.

---

#### Files and docs
- [ ] Files and docs

**Sigil's Answer:** **NONE** — No files collected.

**Note:** Mnemonic backup (12-word phrase) stored encrypted in Android Keystore, never transmitted or shared.

---

#### Calendar
- [ ] Calendar events

**Sigil's Answer:** **NONE** — No calendar data collected.

---

#### Contacts
- [ ] Contacts

**Sigil's Answer:** **NONE** — No contacts collected.

---

#### App activity
- [ ] App interactions
- [ ] In-app search history
- [ ] Installed apps
- [ ] Other user-generated content
- [ ] Other actions

**Sigil's Answer:** **NONE** — No usage tracking, analytics, or activity monitoring.

**Rationale:** 
- No Firebase Analytics
- No Google Analytics
- No third-party tracking SDKs
- Open source AGPL-3.0 — all code auditable

---

#### Web browsing
- [ ] Web browsing history

**Sigil's Answer:** **NONE** — No web browsing data collected.

---

#### App info and performance
- [ ] Crash logs
- [ ] Diagnostics
- [ ] Other app performance data

**Sigil's Answer:** **NONE by default**

**Future (opt-in):** If crash reporting enabled:
- Crash logs: stack traces, device model, OS version
- NOT linked to user identity (no fingerprint in reports)
- Purpose: App performance improvement

**Current status:** Not yet implemented.

---

### Section 3: Data Security

#### Is all user data encrypted in transit?

**YES** — All network communication uses TLS 1.3.

**Details:**
- Challenge delivery: HTTPS or WebSocket Secure (WSS)
- Response transmission: HTTPS POST with TLS 1.3
- Relay communication: WSS to relay.sigilauth.com

**Source:** `android/app/src/main/kotlin/com/sigilauth/app/network/ApiClient.kt` — OkHttp with TLS 1.3

---

#### Can users request data deletion?

**YES** — Users can unpair device from server, triggering deletion.

**Process:**
1. User taps "Unpair" in app
2. App sends DELETE request to server
3. Server deletes device public key + fingerprint from database
4. App deletes local keypair + mnemonic

**Verification:** Protocol spec at `api/openapi.yaml` — DELETE `/devices/:fingerprint`

---

### Section 4: Data Usage and Handling

For **Device or other IDs** (only data type collected):

#### Is this data collected?
**YES**

#### Is this data shared with third parties?
**NO**

**Clarification:** Data sent only to servers user explicitly pairs with. Those are user-chosen endpoints, not "third parties" in Google's definition. Sigil Auth itself does not share data.

#### Is this data processed ephemerally?
**NO** — Fingerprint persisted for ongoing authentication.

#### Is data collection required or optional?
**REQUIRED** — Core app functionality depends on cryptographic identity.

#### Why is this data collected?
- [x] App functionality
- [ ] Analytics
- [ ] Developer communications
- [ ] Advertising or marketing
- [ ] Fraud prevention, security, and compliance
- [ ] Personalization
- [ ] Account management

**Explanation:** Device fingerprint is cryptographic identity used for signing authentication challenges. App cannot function without it.

---

## Privacy Policy

**URL:** https://sigilauth.com/privacy

**Required content:**
- What data collected: Device fingerprint only
- How used: Cryptographic authentication
- How shared: Sent to servers user pairs with
- How deleted: Unpair device
- User rights: Delete app, unpair servers
- Contact: privacy@sigilauth.com

---

## Data Safety Section Preview (as shown in Play Store)

**Data collected and shared:**
> Device or other IDs  
> Used for App functionality  
> Data is encrypted in transit  
> You can request that data be deleted

**No data collected:**
> Location, Personal info (other), Financial info, Health and fitness, Messages, Photos and videos, Audio files, Files and docs, Calendar, Contacts, App activity, Web browsing, App info and performance

---

## Verification

**How users can verify our claims:**

1. **Open source:** Clone repo and audit code
   ```bash
   git clone https://github.com/sigilauth/app
   grep -r "Analytics\|Firebase\|tracking" android/
   # Result: No analytics SDKs
   ```

2. **Check dependencies:** `android/app/build.gradle.kts`
   - Result: Zero third-party analytics libraries

3. **Network inspection:** Use HTTP proxy (Charles, mitmproxy) to inspect traffic
   - Result: Only HTTPS to user-paired servers + relay.sigilauth.com (push delivery)

4. **Protocol audit:** `api/openapi.yaml` documents all endpoints
   - Only data sent: public key (registration), signed challenges (auth)

**License:** AGPL-3.0 — full source code transparency

---

## Compliance Notes

**GDPR (EU):**
- Data minimization: Only collect fingerprint (required)
- Purpose limitation: Used only for authentication
- Right to deletion: Unpair feature
- Data portability: Mnemonic backup (user controls)

**CCPA (California):**
- No sale of personal information
- No sharing for advertising
- User can delete data (unpair)

**COPPA (Children):**
- Sigil Auth not directed at children under 13
- No special collection for children
- Recommend age gate if needed

---

**Last Updated:** 2026-04-26  
**Prepared for:** Play Console Data Safety submission  
**Contact:** privacy@sigilauth.com
