# App Store Connect Setup Checklist

**App:** Sigil Auth  
**Bundle ID:** com.wagmilabs.sigil  
**App ID:** 6763482460  
**Date:** 2026-04-26

---

## Legal Entity Information

**Developer Name:** Wagmi Labs Pty Ltd  
**ABN:** 69 696 024 987  
**ACN:** 696 024 987  
**Entity Type:** Australian Proprietary Limited Company

**Registered Address:**  
Brisbane, Queensland  
Australia

**Contact:**  
**Email:** admin@wagmilabs.vc  
**Phone:** (to be provided)

---

## Agreements & Contracts

### Apple Developer Program

**Account Holder:** Kaity  
**Apple ID:** kaity@wagmilabs.vc  
**Team ID:** CVBUNQ5UY3  
**Team Name:** Wagmi Labs LLC

**Status:** ✓ Enrolled  
**Annual Fee:** Paid

---

## Banking & Tax Information

### Banking (for App Store proceeds)

**Status:** ⚠️ Pending — Kaity to complete

**Required fields:**
- Account holder name
- Bank name  
- Account number
- Routing number (Swift/BIC for international)
- Account type (checking/savings)

**Note:** Even if app is free, banking info required for account activation.

---

### Tax Information

#### Australia (Country of Residence)

**Tax ID Type:** ABN (Australian Business Number)  
**Tax ID:** 69 696 024 987

**Tax Treaty:** Australia-US tax treaty exists (check withholding rate)

#### United States (App Store Country)

**W-8BEN-E Form Required:** YES (foreign entity)

**Tax Information to Provide:**
- Legal name: Wagmi Labs Pty Ltd
- Country of incorporation: Australia
- ABN: 69 696 024 987
- Permanent address: (Brisbane address)
- Tax treaty benefits: Claim if applicable

**Withholding Tax:** 
- Default: 30% for non-treaty
- With treaty: potentially reduced (check AU-US treaty terms)

---

## Export Compliance

### Is Your App Exempt from US Export Regulations?

**Question:** Does your app use encryption?

**Answer:** YES — Sigil Auth uses ECDSA P-256 and AES-256-GCM

### Encryption Declaration

**Standard Cryptography:** YES  
**Proprietary/Custom Encryption:** NO

**Encryption Used:**
- **ECDSA P-256:** Digital signatures (standard NIST curve)
- **AES-256-GCM:** Symmetric encryption for mnemonic storage
- **HKDF (HMAC-SHA256):** Key derivation
- **TLS 1.3:** Transport encryption

**All cryptography:** Standard, non-proprietary, widely available

### Export Classification

**Likely Classification:** 5D992  
**Rationale:** Mass-market cryptography using standard algorithms

**EAR §740.17(b)(1) Exemption:** **LIKELY YES**

**Criteria:**
- ✓ Uses standard cryptographic algorithms (P-256, AES-256)
- ✓ No key length restrictions exceed mass-market limits
- ✓ Not government/military end-use
- ✓ Publicly available (open source AGPL-3.0)

**Self-Classification Report:** May be required

**Recommendation:** Select "**Yes, uses standard encryption**" in App Store Connect. Apple will guide you through self-assessment questionnaire. Most apps using Apple CryptoKit or standard algorithms qualify for exemption.

**⚠️ Flag for Kaity:** Review EAR exemption criteria or consult export compliance specialist if uncertain. Self-classification errors can delay app approval.

---

## App Information

### Basic Details

**App Name:** Sigil Auth  
**Subtitle:** Hardware-backed strong authentication  
**Primary Category:** Utilities  
**Secondary Category:** Developer Tools (optional)

**Content Rating:** 4+ (no objectionable content)

**Demo Account:** Not required (app doesn't require login)

---

### App Privacy

**Privacy Policy URL:** https://sigilauth.com/privacy

**Privacy Nutrition Labels:** See `ios/PRIVACY-NUTRITION-LABELS.md`

**Summary:**
- Data collected: Device ID (cryptographic fingerprint) only
- Purpose: App functionality (authentication)
- No tracking, no third-party sharing
- Open source AGPL-3.0

---

## TestFlight Setup

### External Testing

**Test Groups:**  
- Internal testers (Wagmi Labs team)
- External beta testers (early adopters, security researchers)

**Beta App Description:**  
Use metadata from `ios/fastlane/metadata/en-US/description.txt`

**What to Test:**
- Pairing flow (QR, 8-digit code, manual)
- Push notification delivery
- Biometric approval (Face ID / Touch ID)
- Mnemonic backup and recovery
- Pictogram verification

**Feedback Email:** beta@sigilauth.com (or support@sigilauth.com)

---

## Pre-Submission Checklist

### Required Assets

- [x] App icons (all sizes via Assets.xcassets)
- [x] Metadata (name, subtitle, description, keywords)
- [ ] Screenshots (pending generation — see `ios/fastlane/screenshots/README.md`)
- [x] Privacy policy URL
- [x] Privacy Nutrition Labels documentation

### Required Information

- [x] Legal entity (Wagmi Labs Pty Ltd)
- [x] ABN/ACN
- [ ] Banking information (Kaity to complete)
- [ ] Tax information (W-8BEN-E form)
- [x] Export compliance self-assessment

### Build Requirements

- [x] IPA built with App Store distribution profile
- [x] IPA location: `~/Desktop/SigilAuth_AppStore/SigilAuth.ipa`
- [x] Upload command: `make ios-upload` or `fastlane ios_beta`
- [x] Provisioning profile verified (get-task-allow=false)

---

## First-Time Submission Steps

1. **Complete banking + tax info** in App Store Connect (Agreements, Tax, Banking)
2. **Create app listing** (if not exists):
   - Name: Sigil Auth
   - Bundle ID: com.wagmilabs.sigil
   - SKU: sigil-auth-ios
3. **Upload build:**
   ```bash
   export APPLE_ID=kaity@wagmilabs.vc
   export SIGIL_TESTFLIGHT_APPPW=<password>
   fastlane ios_beta
   ```
4. **Answer questionnaires:**
   - Export compliance (use encryption → standard algorithms → exempt)
   - Privacy Nutrition Labels (copy from PRIVACY-NUTRITION-LABELS.md)
   - Content Rights (confirm you have rights to all content)
   - Advertising Identifier (NO — app doesn't use IDFA)
5. **Add screenshots** (once generated)
6. **Submit for TestFlight review** (not App Store yet)
7. **Test with external testers**
8. **Iterate based on feedback**

---

## Ongoing Maintenance

### Metadata Updates

```bash
# Edit metadata files
vim ios/fastlane/metadata/en-US/description.txt

# Push to App Store Connect
fastlane ios metadata
```

### Build Updates

```bash
# Increment build number in Project.yml
# Build + upload
fastlane ios_beta
```

### TestFlight Distribution

- Add testers via App Store Connect
- Testers receive email with TestFlight link
- 90-day beta period per build

---

## Support Contacts

**App Store Connect Issues:**  
https://developer.apple.com/contact/

**Export Compliance Questions:**  
https://www.bis.doc.gov/

**Tax/Banking Questions:**  
Consult Australian accountant familiar with US tax treaties

**Wagmi Labs Contact:**  
admin@wagmilabs.vc

---

**Prepared:** 2026-04-26  
**Last Updated:** 2026-04-26  
**Next Review:** Before first App Store submission (post-TestFlight)
