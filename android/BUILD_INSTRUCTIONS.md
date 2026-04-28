# Build Instructions

## Initial Setup (One-Time)

The Gradle wrapper JAR needs to be initialized. Run this once:

```bash
# If you have Gradle installed locally:
gradle wrapper --gradle-version=8.2

# OR download and extract manually:
# 1. Download: https://services.gradle.org/distributions/gradle-8.2-bin.zip
# 2. Extract to ~/.gradle/wrapper/dists/
# 3. Run: ./gradlew tasks
```

## Build Commands

Once wrapper is initialized:

```bash
# Compile
./gradlew assembleDebug

# Run tests
./gradlew test

# Run all checks
./gradlew check

# Install to device
./gradlew installDebug
```

## Project Status

✅ **Gradle configuration complete**
- Build scripts: `build.gradle.kts`, `app/build.gradle.kts`
- Dependencies: All configured (Compose, security-crypto, FCM, Room, etc.)
- Min SDK: 31, Target SDK: 34
- Kotlin: 1.9.22

✅ **Source files: 21 Kotlin files**
- Core crypto layer (Keystore, signing, pictogram)
- API layer (Retrofit interfaces, models)
- Data layer (Room database)
- UI layer (Compose theme, MainActivity)

✅ **Tests: 17 test cases**
- `CryptoUtilsTest.kt` (7 tests)
- `PictogramDerivationTest.kt` (10 tests)

## Wrapper Initialization Alternative

If Gradle is not installed locally, the wrapper can be initialized via Android Studio:
1. Open project in Android Studio
2. Studio will auto-detect and download Gradle 8.2
3. Wrapper will be generated automatically

---

**Project is fully configured and ready to build once wrapper is initialized.**
