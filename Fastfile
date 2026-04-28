# Fastlane configuration for Sigil Auth mobile apps
#
# Usage:
#   fastlane ios_beta        - Build, upload IPA, deliver metadata to TestFlight
#   fastlane android_internal - Build AAB, upload to Play Internal Testing
#   fastlane metadata        - Push metadata + screenshots without binary changes
#   fastlane validate        - Validate Fastlane setup without uploading

default_platform(:ios)

# iOS Lanes
platform :ios do
  desc "Build and upload iOS app to TestFlight"
  desc "Requires: APPLE_ID, SIGIL_TESTFLIGHT_APPPW environment variables"
  lane :ios_beta do
    # Verify required env vars
    ensure_env_vars(
      env_vars: ['APPLE_ID', 'SIGIL_TESTFLIGHT_APPPW']
    )

    # Build + export IPA using Makefile
    sh("cd .. && make ios-upload")

    # Deliver metadata + screenshots to App Store Connect
    # Note: Binary upload handled by Makefile (xcrun altool)
    # This just updates metadata fields
    deliver(
      skip_binary_upload: true,
      skip_screenshots: Dir["ios/fastlane/screenshots/en-US/*.png"].empty?,
      skip_metadata: false,
      force: true,
      submit_for_review: false,
      automatic_release: false,
      platform: "ios",
      app_identifier: "com.wagmilabs.sigil",
      username: ENV['APPLE_ID']
    )
  end

  desc "Push metadata and screenshots only (no binary)"
  lane :metadata do
    deliver(
      skip_binary_upload: true,
      skip_screenshots: Dir["ios/fastlane/screenshots/en-US/*.png"].empty?,
      skip_metadata: false,
      force: true,
      submit_for_review: false,
      platform: "ios",
      app_identifier: "com.wagmilabs.sigil"
    )
  end

  desc "Validate Fastlane setup without uploading"
  lane :validate do
    UI.message("🔍 Validating iOS Fastlane configuration...")

    # Check metadata files exist
    required_metadata = [
      "ios/fastlane/metadata/en-US/name.txt",
      "ios/fastlane/metadata/en-US/description.txt",
      "ios/fastlane/metadata/en-US/keywords.txt"
    ]

    required_metadata.each do |file|
      unless File.exist?("../#{file}")
        UI.user_error!("❌ Missing metadata file: #{file}")
      end
    end

    # Check for screenshots (warning only)
    if Dir["../ios/fastlane/screenshots/en-US/*.png"].empty?
      UI.important("⚠️  No screenshots found at ios/fastlane/screenshots/en-US/")
      UI.important("   Screenshots will be skipped during upload")
    else
      UI.success("✓ Screenshots found")
    end

    # Check env vars (for beta lane)
    if ENV['APPLE_ID'].nil?
      UI.important("⚠️  APPLE_ID not set (required for ios_beta lane)")
    else
      UI.success("✓ APPLE_ID set")
    end

    if ENV['SIGIL_TESTFLIGHT_APPPW'].nil?
      UI.important("⚠️  SIGIL_TESTFLIGHT_APPPW not set (required for ios_beta lane)")
    else
      UI.success("✓ SIGIL_TESTFLIGHT_APPPW set")
    end

    UI.success("🎉 iOS Fastlane configuration valid")
  end
end

# Android Lanes
platform :android do
  desc "Build and upload Android app to Play Console Internal Testing"
  desc "Requires: SIGIL_KEYSTORE_PASSWORD, SIGIL_KEY_PASSWORD environment variables"
  lane :android_internal do
    # Verify required env vars
    ensure_env_vars(
      env_vars: ['SIGIL_KEYSTORE_PASSWORD', 'SIGIL_KEY_PASSWORD']
    )

    # Build release AAB using Makefile
    sh("cd .. && make android-bundle")

    # Upload to Play Console Internal Testing track
    supply(
      track: 'internal',
      aab: 'android/app/build/outputs/bundle/release/app-release.aab',
      package_name: 'com.wagmilabs.sigil',
      metadata_path: 'android/fastlane/metadata/android',
      skip_upload_apk: true,
      skip_upload_metadata: Dir["android/fastlane/metadata/android/en-US/*.txt"].empty?,
      skip_upload_images: Dir["android/fastlane/metadata/android/en-US/images/**/*.png"].empty?,
      skip_upload_screenshots: Dir["android/fastlane/metadata/android/en-US/images/phoneScreenshots/*.png"].empty?,
      json_key_data: ENV['PLAY_STORE_JSON_KEY']  # Optional: use service account
    )
  end

  desc "Push metadata and screenshots only (no binary)"
  lane :metadata do
    supply(
      track: 'internal',
      package_name: 'com.wagmilabs.sigil',
      metadata_path: 'android/fastlane/metadata/android',
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_metadata: false,
      skip_upload_images: false,
      skip_upload_screenshots: false
    )
  end

  desc "Validate Fastlane setup without uploading"
  lane :validate do
    UI.message("🔍 Validating Android Fastlane configuration...")

    # Check metadata files exist
    required_metadata = [
      "android/fastlane/metadata/android/en-US/title.txt",
      "android/fastlane/metadata/android/en-US/short_description.txt",
      "android/fastlane/metadata/android/en-US/full_description.txt"
    ]

    required_metadata.each do |file|
      unless File.exist?("../#{file}")
        UI.user_error!("❌ Missing metadata file: #{file}")
      end
    end

    # Check for screenshots (warning only)
    if Dir["../android/fastlane/metadata/android/en-US/images/phoneScreenshots/*.png"].empty?
      UI.important("⚠️  No phone screenshots found")
      UI.important("   Screenshots will be skipped during upload")
    else
      UI.success("✓ Phone screenshots found")
    end

    # Check env vars
    if ENV['SIGIL_KEYSTORE_PASSWORD'].nil?
      UI.important("⚠️  SIGIL_KEYSTORE_PASSWORD not set (required for android_internal)")
    else
      UI.success("✓ SIGIL_KEYSTORE_PASSWORD set")
    end

    UI.success("🎉 Android Fastlane configuration valid")
  end
end

# Cross-platform Lanes
desc "Validate all Fastlane configurations"
lane :validate_all do
  ios_validate
  android_validate
  UI.success("🎉 All Fastlane configurations valid")
end

# Helper: Ensure environment variables are set
private_lane :ensure_env_vars do |options|
  options[:env_vars].each do |var|
    UI.user_error!("❌ Environment variable #{var} is not set") if ENV[var].nil? || ENV[var].empty?
  end
end
