# PoopyFeed Android

Native Android app for PoopyFeed built with Kotlin and Jetpack
Compose. Connects to the Django REST API backend for tracking
child feedings, diapers, naps, and sharing child profiles with
caregivers.

## Tech Stack

- Kotlin 2.1 with Android SDK 35 (minSdk 26)
- Jetpack Compose with Material 3
- Hilt for dependency injection
- Retrofit + OkHttp for networking
- Coroutines + StateFlow for async/reactive state
- DataStore for local token persistence
- Gradle 8.11.1 (Kotlin DSL)

## Development

### Android Studio

Open the `android/` directory in Android Studio, sync Gradle,
and run on an emulator or device.

```bash
./gradlew build              # Build APK
./gradlew test               # Run unit tests
./gradlew spotlessApply      # Format Kotlin code
./gradlew jacocoTestReport   # Generate coverage report
```

### Container Builds

Build and test without Android Studio using containers (Podman by default):

```bash
make build          # Build debug APK
make test           # Run unit tests
make test-coverage  # Run tests with JaCoCo coverage
make lint           # Run lint checks
make format         # Format Kotlin code (ktlint via Spotless)
make clean          # Clean build artifacts
```

From the root directory (via podman-compose):

```bash
make build-android          # Build debug APK
make test-android           # Run unit tests
make test-android-coverage  # Run tests with coverage
make lint-android           # Run lint checks
make format-android         # Format Kotlin code
```

### Running a Single Test

```bash
# Specific test class
./gradlew testDebugUnitTest --tests=com.poopyfeed.android.ui.screens.login.LoginViewModelTest

# Specific test method
./gradlew testDebugUnitTest --tests=com.poopyfeed.android.ui.screens.login.LoginViewModelTest.testLoginSuccess

# Wildcard matching
./gradlew testDebugUnitTest --tests=*LoginViewModel*
```

### Pre-commit Hooks

```bash
make pre-commit-setup
```

Enforces conventional commits, trailing whitespace, file
endings, spell checking, and more.

## API Configuration

The backend API URL is set in `app/build.gradle.kts`:

- **Emulator**: `http://10.0.2.2:8000` (default — maps to host localhost)
- **Physical device**: Change `API_BASE_URL` to your machine's IP and rebuild

## Architecture

The app follows Clean Architecture with MVVM:

```text
UI (Compose Screens + ViewModels)
    ↓
Repositories (Result<T> error handling)
    ↓
Data Sources (Retrofit APIs / DataStore)
```

See [CLAUDE.md](CLAUDE.md) for detailed architecture, patterns, and testing strategy.
