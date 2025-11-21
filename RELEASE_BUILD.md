# Release Build Guide

This document describes how to build a release version of the Short Video Blocker app.

## Prerequisites

1. Android Studio (latest stable version)
2. JDK 17 or higher
3. A release keystore file (for signing the APK/AAB)

## Version Management

The app version is managed in `app/build.gradle.kts`:

```kotlin
versionCode = 1        // Increment for each release
versionName = "1.0.0"  // Semantic versioning: MAJOR.MINOR.PATCH
```

### Version Guidelines

- **versionCode**: Must be incremented for every release to Google Play
- **versionName**: Follow semantic versioning:
  - MAJOR: Breaking changes or major feature releases
  - MINOR: New features, backward compatible
  - PATCH: Bug fixes and minor improvements

## Keystore Setup

### Creating a Release Keystore

If you don't have a release keystore, create one:

```bash
keytool -genkey -v -keystore release.keystore -alias shortblocker -keyalg RSA -keysize 2048 -validity 10000
```

**Important**: Store your keystore file and passwords securely! If you lose them, you cannot update your app on Google Play.

### Configuring Keystore

1. Copy the template file:
   ```bash
   cp keystore.properties.template keystore.properties
   ```

2. Edit `keystore.properties` with your actual values:
   ```properties
   storeFile=/path/to/your/release.keystore
   storePassword=your_keystore_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```

3. Update `app/build.gradle.kts` to load from keystore.properties:
   ```kotlin
   // Load keystore properties
   val keystorePropertiesFile = rootProject.file("keystore.properties")
   val keystoreProperties = Properties()
   if (keystorePropertiesFile.exists()) {
       keystoreProperties.load(FileInputStream(keystorePropertiesFile))
   }

   signingConfigs {
       create("release") {
           storeFile = file(keystoreProperties["storeFile"] ?: "debug.keystore")
           storePassword = keystoreProperties["storePassword"] as String?
           keyAlias = keystoreProperties["keyAlias"] as String?
           keyPassword = keystoreProperties["keyPassword"] as String?
       }
   }
   ```

## Building Release APK/AAB

### Using Android Studio

1. Select **Build > Generate Signed Bundle / APK**
2. Choose **Android App Bundle** (recommended for Play Store) or **APK**
3. Select your keystore file and enter credentials
4. Choose **release** build variant
5. Click **Finish**

### Using Command Line

#### Build Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

#### Build Release AAB (for Play Store)
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

#### Build Staging (for testing)
```bash
./gradlew assembleStaging
```

## ProGuard/R8 Configuration

The release build uses R8 for code shrinking and obfuscation. Configuration is in:
- `app/proguard-rules.pro` - Custom rules for this project

### Key ProGuard Rules

- **Room entities**: Preserved to prevent database issues
- **Kotlin reflection**: Kept for proper Kotlin functionality
- **Accessibility Service**: Preserved for Android system integration
- **Logging**: Debug logs removed in release builds

### Testing ProGuard Build

Always test the release build thoroughly:

```bash
# Install release build on device
./gradlew installRelease

# Or install staging build (has debug enabled)
./gradlew installStaging
```

## Build Variants

| Variant | Purpose | Minify | Debuggable | Suffix |
|---------|---------|--------|------------|--------|
| debug | Development | No | Yes | .debug |
| staging | Pre-release testing | Yes | Yes | .staging |
| release | Production | Yes | No | - |

## Pre-Release Checklist

Before releasing to production:

- [ ] Increment `versionCode` and `versionName`
- [ ] Test release build on multiple devices
- [ ] Verify all features work with ProGuard enabled
- [ ] Check app size (should be < 5MB)
- [ ] Test accessibility service functionality
- [ ] Verify all platforms (YouTube, Instagram, TikTok) are detected correctly
- [ ] Test overlay display and permissions
- [ ] Review ProGuard mapping file for any issues
- [ ] Update CHANGELOG.md with release notes
- [ ] Create git tag for release: `git tag -a v1.0.0 -m "Release 1.0.0"`

## Troubleshooting

### ProGuard Issues

If the app crashes in release but works in debug:

1. Check the ProGuard mapping file: `app/build/outputs/mapping/release/mapping.txt`
2. Add necessary `-keep` rules to `proguard-rules.pro`
3. Test with staging build (has debugging enabled)

### Common ProGuard Problems

- **Room crashes**: Ensure entity classes are kept
- **Reflection errors**: Add `-keep` rules for reflected classes
- **Kotlin coroutines**: Verify coroutines rules are present

### Signing Issues

If signing fails:

1. Verify keystore file path is correct
2. Check passwords in `keystore.properties`
3. Ensure keystore file has proper permissions

## CI/CD Integration

For automated builds, set keystore properties as environment variables:

```bash
export KEYSTORE_FILE=/path/to/release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=your_alias
export KEY_PASSWORD=your_key_password
```

Then update `build.gradle.kts` to read from environment variables.

## Security Notes

- **Never commit** `keystore.properties` or keystore files to version control
- Store keystore and passwords in a secure location (password manager, CI/CD secrets)
- Keep a backup of your keystore in a secure location
- Use different keystores for debug and release builds

## Resources

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [ProGuard/R8 Documentation](https://developer.android.com/studio/build/shrink-code)
- [Semantic Versioning](https://semver.org/)
