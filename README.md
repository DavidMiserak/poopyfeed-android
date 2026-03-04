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
- Gradle 8.10 (Kotlin DSL)

## Development

### Android Studio

1. **Open the Android project only**
   In Android Studio: **File → Open** and select the **`android`** folder (e.g. `path/to/poopyfeed/android`).
   Do **not** open the repo root `poopyfeed` — the app is only in the `android` subfolder.

2. **Let Gradle sync**
   Android Studio will create `local.properties` with `sdk.dir` if needed.
   Wait for sync to finish.

3. **Gradle JDK**
   Use **JDK 17** for Gradle: **File → Settings → Build, Execution, Deployment
   → Build Tools → Gradle** → set **Gradle JDK** to 17 (or "Embedded JDK" if
   it is 17+).

4. **Run**
   Choose an emulator or connected device and click **Run** (green triangle).
   If "Run" is disabled, add a device (AVD Manager or USB) and select the
   `app` run configuration.

If sync fails, try **File → Invalidate Caches → Invalidate and Restart**.
If Run fails, check the **Build** and **Run** tool windows for errors.

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

## API Configuration (localhost:8000)

The app talks to your backend at **localhost:8000** on your machine.

- **Emulator (default):** Uses `http://10.0.2.2:8000`. The emulator maps
  `10.0.2.2` to your host's localhost. Start the backend (e.g. `make run`
  from repo root), then run the app. No extra setup.
- **Physical device:** In `android/local.properties` add
  `api.base.url=http://YOUR_IP:8000`, then rebuild. Find your IP with
  `ip addr` or `ipconfig`.

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
