# iOS Deployment Quick Reference

Two commands for deploying Sigil Auth iOS app:

---

## Fast Iteration (Install to Connected iPhone)

```bash
cd /Volumes/Expansion/src/sigilauth/app/ios
fastlane install_dev
```

**What it does:**
- Builds debug configuration
- Uses automatic development signing
- Installs directly to connected iPhone via USB
- **~2 min** end-to-end

**Use when:** Testing changes quickly on your device

---

## TestFlight Beta Upload

```bash
cd /Volumes/Expansion/src/sigilauth/app/ios
fastlane beta
```

**What it does:**
- Auto-increments build number
- Builds release configuration with App Store signing
- Uploads to TestFlight
- **~5 min** build + upload, then 5-10 min Apple processing

**Use when:** Sharing beta with testers or preparing App Store release

**Check status:** https://appstoreconnect.apple.com/apps/6763482460

---

## Prerequisites

### For `install_dev`:
- iPhone connected via USB
- Trust established ("Trust This Computer" on iPhone)
- Automatic signing configured in Xcode project

### For `beta`:
- App Store Connect API key set up (see `../APP-STORE-API-KEY.md`)
- Key file at `~/.appstoreconnect/api/AuthKey_*.p8` or `~/.private_keys/AuthKey_*.p8`
- Xcode configured with Apple ID signed in

---

## Troubleshooting

**"No devices found"** (install_dev):
- Reconnect iPhone
- Check iPhone shows "Trust This Computer" prompt
- Run `ios-deploy -c` to list connected devices

**"Invalid credentials"** (beta):
- Verify API key file exists and is readable (`chmod 600`)
- Check key ID and issuer ID in `Appfile`
- See `../APP-STORE-API-KEY.md` for full setup

**"Signing failed"**:
- Open Xcode, go to Signing & Capabilities
- Ensure Team is set to "Wagmi Labs Pty Ltd"
- Re-download provisioning profiles if needed

---

## Build Artifacts

Both lanes output to `./build/`:
- `install_dev` → `SigilAuth-Dev.ipa` (development signed)
- `beta` → `SigilAuth.ipa` (App Store signed)
