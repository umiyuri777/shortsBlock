# Short Video Blocker

An Android application that selectively blocks short-form video content (YouTube Shorts, Instagram Reels, TikTok) while allowing access to regular content.

## Project Structure

```
app/src/main/java/com/example/shortblocker/
├── service/     # AccessibilityService and related components
├── detector/    # Platform-specific detection logic
├── action/      # Block action implementations
├── config/      # Configuration management
├── data/        # Data models, Room database, repositories
└── ui/          # Activities, fragments, ViewModels
```

## Requirements

- Android Studio Hedgehog or later
- Kotlin 1.9.20
- minSdk 24 (Android 7.0)
- targetSdk 34 (Android 14)

## Dependencies

- **Room**: Local database for settings and logs
- **Coroutines**: Asynchronous programming
- **Hilt**: Dependency injection
- **Material Components**: UI components

## Build

```bash
./gradlew build
```

## Features

- Detects and blocks YouTube Shorts
- Detects and blocks Instagram Reels
- Blocks TikTok app entirely
- Customizable block actions (overlay, navigate back, notification)
- Temporary disable functionality
- Usage statistics

## Architecture

The app uses AccessibilityService to monitor screen content and detect short-form video sections. When detected, it executes the configured block action.

See `.kiro/specs/short-video-blocker/design.md` for detailed architecture documentation.
