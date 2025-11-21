# Dependency Injection Setup

This document describes the Hilt dependency injection setup for the Short Video Blocker app.

## Overview

The app uses Dagger Hilt for dependency injection, which provides:

- Automatic dependency management
- Singleton scoping for shared instances
- ViewModel injection support
- Service injection support

## Modules

### 1. DatabaseModule

**Location:** `data/DatabaseModule.kt`

Provides:

- `AppDatabase` - Room database instance
- `SettingsDao` - Settings data access object
- `BlockLogDao` - Block log data access object
- `SharedPreferences` - Android shared preferences
- `SettingsRepository` - Settings repository implementation

### 2. ConfigurationModule

**Location:** `config/ConfigurationModule.kt`

Provides:

- `ConfigurationManager` - Configuration management
- `TemporaryDisableManager` - Temporary disable functionality

### 3. ErrorModule

**Location:** `error/ErrorModule.kt`

Provides:

- `SafeModeManager` - Safe mode management
- `ErrorHandler` - Error handling
- `BlockerLogger` - Logging functionality

### 4. DetectorModule

**Location:** `detector/DetectorModule.kt`

Provides:

- `PlatformDetectorManager` - Platform detection coordination

Note: Individual detectors (YouTubeDetector, InstagramDetector, TikTokDetector) are created internally by PlatformDetectorManager.

### 5. ServiceModule

**Location:** `service/ServiceModule.kt`

Provides:

- `AccessibilityEventProcessor` - Event processing
- `EventDebouncer` - Event debouncing

### 6. UIModule

**Location:** `ui/UIModule.kt`

Provides:

- `StatisticsManager` - Statistics calculation and aggregation

### 7. ActionModule

**Location:** `action/ActionModule.kt`

Note: This module is empty because `BlockActionManager` requires an `AccessibilityService` instance which cannot be injected via Hilt. It must be created manually in `BlockerAccessibilityService.onServiceConnected()`.

## Component Integration

### Application Class

**File:** `ShortBlockerApplication.kt`

```kotlin
@HiltAndroidApp
class ShortBlockerApplication : Application()
```

The `@HiltAndroidApp` annotation triggers Hilt's code generation and sets up the application-level dependency container.

### Activities

#### MainActivity

**File:** `ui/MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var blockLogDao: BlockLogDao
    @Inject lateinit var sharedPreferences: SharedPreferences
}
```

#### SettingsActivity

**File:** `ui/SettingsActivity.kt`

```kotlin
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var blockLogDao: BlockLogDao
    @Inject lateinit var settingsRepository: SettingsRepository
}
```

### ViewModel

#### MainViewModel

**File:** `ui/MainViewModel.kt`

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager,
    private val statisticsManager: StatisticsManager
) : ViewModel()
```

ViewModels use constructor injection with the `@HiltViewModel` annotation.

### AccessibilityService

#### BlockerAccessibilityService

**File:** `service/BlockerAccessibilityService.kt`

```kotlin
@AndroidEntryPoint
class BlockerAccessibilityService : AccessibilityService() {
    @Inject lateinit var eventProcessor: AccessibilityEventProcessor
    @Inject lateinit var eventDebouncer: EventDebouncer
    @Inject lateinit var platformDetectorManager: PlatformDetectorManager
    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var errorHandler: ErrorHandler

    private lateinit var blockActionManager: BlockActionManager

    override fun onServiceConnected() {
        // Manual creation because it needs the service instance
        blockActionManager = BlockActionManager(
            context = applicationContext,
            accessibilityService = this,
            settingsRepository = settingsRepository
        )
    }
}
```

AccessibilityService uses field injection for most dependencies, but `BlockActionManager` must be created manually because it requires the service instance.

## Scoping

All provided dependencies use `@Singleton` scope, meaning:

- Only one instance exists throughout the app lifecycle
- Instances are shared across all injection points
- Memory efficient and ensures consistent state

## Constructor Injection

Most classes use constructor injection with `@Inject`:

```kotlin
@Singleton
class ConfigurationManagerImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ConfigurationManager
```

This is the preferred injection method as it:

- Makes dependencies explicit
- Enables easier testing
- Provides compile-time safety

## Testing Considerations

Hilt provides testing support through:

- `@HiltAndroidTest` for instrumented tests
- `@UninstallModules` to replace modules in tests
- Test-specific modules for mocking dependencies

## Build Configuration

### Root build.gradle.kts

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

### App build.gradle.kts

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

### AndroidManifest.xml

```xml
<application
    android:name=".ShortBlockerApplication"
    ...>
```

The custom Application class must be registered in the manifest.

## Summary

The DI setup provides:

- ✅ Centralized dependency management
- ✅ Singleton scoping for shared state
- ✅ ViewModel injection support
- ✅ Service injection support
- ✅ Easy testing with mock dependencies
- ✅ Compile-time dependency validation
- ✅ Reduced boilerplate code
