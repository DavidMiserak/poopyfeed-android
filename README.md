# PoopyFeed Android

Native Android app for PoopyFeed built with Kotlin and Jetpack Compose.

## Tech Stack

- Kotlin 2.1
- Jetpack Compose with Material 3
- Gradle 8.11.1 (Kotlin DSL)

## Development

### Android Studio

Open the `android/` directory in Android Studio, sync Gradle, and run on an
emulator or device.

### Container Builds

Build and test without Android Studio using containers:

```bash
make image-build   # Build container image
make build         # Build debug APK
make test          # Run unit tests
make lint          # Run lint checks
```

### Pre-commit Hooks

```bash
make pre-commit-setup
```
