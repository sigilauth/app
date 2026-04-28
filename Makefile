# Sigil Auth Mobile Build Automation
#
# Required environment variables:
#   - APPLE_ID: Apple Developer account email (e.g., kaity@wagmilabs.vc)
#   - SIGIL_TESTFLIGHT_APPPW: App-specific password for altool uploads
#   - SIGIL_KEYSTORE_PASSWORD: Android release keystore password (for signing)
#   - SIGIL_KEY_PASSWORD: Android key password (usually same as keystore password)
#
# Android keystore location:
#   - Expected at: android/app/release.keystore
#   - Generate with: keytool -genkeypair -v -keystore android/app/release.keystore \
#                    -alias sigil -keyalg RSA -keysize 4096 -validity 10000

.PHONY: help ios-archive ios-export-appstore ios-upload android-bundle android-upload clean

help:
	@echo "Sigil Auth Mobile Build Targets"
	@echo ""
	@echo "iOS:"
	@echo "  make ios-archive          - Build Xcode archive"
	@echo "  make ios-export-appstore  - Export IPA for App Store distribution"
	@echo "  make ios-upload           - Upload IPA to TestFlight (requires APPLE_ID, SIGIL_TESTFLIGHT_APPPW)"
	@echo ""
	@echo "Android:"
	@echo "  make android-bundle       - Build release AAB (requires keystore + env vars)"
	@echo "  make android-upload       - Instructions for Play Console upload"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean                - Remove build artifacts"
	@echo ""
	@echo "Required env vars:"
	@echo "  APPLE_ID                  - Apple Developer account email"
	@echo "  SIGIL_TESTFLIGHT_APPPW    - App-specific password for altool"
	@echo "  SIGIL_KEYSTORE_PASSWORD   - Android release keystore password"
	@echo "  SIGIL_KEY_PASSWORD        - Android key password"

# iOS targets
ios-archive:
	@echo "==> Generating Xcode project with xcodegen..."
	cd ios && xcodegen generate
	@echo "==> Building iOS archive..."
	xcodebuild -project ios/SigilAuth.xcodeproj \
		-scheme SigilAuth \
		-configuration Release \
		-archivePath build/SigilAuth.xcarchive \
		archive

ios-export-appstore: ios-archive
	@echo "==> Exporting IPA for App Store distribution..."
	xcodebuild -exportArchive \
		-archivePath build/SigilAuth.xcarchive \
		-exportPath build/SigilAuth_AppStore \
		-exportOptionsPlist ios/ExportOptions-AppStore.plist \
		-allowProvisioningUpdates

ios-upload: ios-export-appstore
	@echo "==> Uploading IPA to TestFlight..."
	@if [ -z "$(APPLE_ID)" ]; then echo "Error: APPLE_ID not set"; exit 1; fi
	@if [ -z "$(SIGIL_TESTFLIGHT_APPPW)" ]; then echo "Error: SIGIL_TESTFLIGHT_APPPW not set"; exit 1; fi
	xcrun altool --upload-app \
		-f build/SigilAuth_AppStore/SigilAuth.ipa \
		-u $(APPLE_ID) \
		-p $(SIGIL_TESTFLIGHT_APPPW) \
		--type ios

# Android targets
android-bundle:
	@echo "==> Building Android release AAB..."
	@if [ -z "$(SIGIL_KEYSTORE_PASSWORD)" ]; then echo "Error: SIGIL_KEYSTORE_PASSWORD not set"; exit 1; fi
	@if [ -z "$(SIGIL_KEY_PASSWORD)" ]; then echo "Error: SIGIL_KEY_PASSWORD not set"; exit 1; fi
	@if [ ! -f android/app/release.keystore ]; then echo "Error: release.keystore not found at android/app/release.keystore"; exit 1; fi
	cd android && ./gradlew bundleRelease

android-upload: android-bundle
	@echo "==> Android AAB ready for upload"
	@echo ""
	@echo "Manual upload steps:"
	@echo "  1. Open https://play.google.com/console"
	@echo "  2. Navigate to Sigil Auth app"
	@echo "  3. Go to: Release > Testing > Internal testing"
	@echo "  4. Create new release"
	@echo "  5. Upload: android/app/build/outputs/bundle/release/app-release.aab"
	@echo ""
	@echo "Or use fastlane (if configured):"
	@echo "  fastlane supply --aab android/app/build/outputs/bundle/release/app-release.aab --track internal"

# Cleanup
clean:
	@echo "==> Cleaning build artifacts..."
	rm -rf build/
	rm -rf ios/SigilAuth.xcodeproj
	cd android && ./gradlew clean
	@echo "==> Clean complete"
