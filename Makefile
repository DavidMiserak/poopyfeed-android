.PHONY: help build build-debug build-release install clean test test-e2e test-e2e-critical lint shell logs format coverage

# Variables
GRADLE = ./gradlew
APK_DEBUG = poopyfeed/app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE = poopyfeed/app/build/outputs/apk/release/app-release-unsigned.apk
PACKAGE_NAME = net.poopyfeed.pf
ACTIVITY = net.poopyfeed.pf.MainActivity

# Default target
help:
	@echo "PoopyFeed Android Build Commands"
	@echo ""
	@echo "Build:"
	@echo "  make build              Build both debug and release APKs"
	@echo "  make build-debug        Build debug APK"
	@echo "  make build-release      Build release APK (unsigned)"
	@echo "  make clean              Clean build artifacts"
	@echo ""
	@echo "Installation & Running:"
	@echo "  make install            Install debug APK on connected device/emulator"
	@echo "  make install-release    Install release APK on connected device/emulator"
	@echo "  make run                Build and install debug APK"
	@echo "  make start              Start app on device (requires installed APK)"
	@echo "  make stop               Stop running app"
	@echo "  make uninstall          Uninstall app from device"
	@echo ""
	@echo "Testing & Quality:"
	@echo "  make test               Run unit tests (JVM + Robolectric)"
	@echo "  make test-e2e           Run all E2E tests on connected device/emulator"
	@echo "  make test-e2e-critical  Run E2E critical-path tests (set E2E_TEST_EMAIL, E2E_TEST_PASSWORD for backend)"
	@echo "  make coverage           Run unit tests + Kover report (85%% line minimum); opens HTML report"
	@echo "  make lint               Run lint checks"
	@echo "  make lint-fix           Attempt to auto-fix lint issues"
	@echo ""
	@echo "Device Management:"
	@echo "  make devices            List connected devices/emulators"
	@echo "  make logs               View logcat output"
	@echo "  make shell              Open adb shell"
	@echo ""
	@echo "Development:"
	@echo "  make format             Format code with Kotlin formatter"
	@echo ""

.PHONY: pre-commit-setup
pre-commit-setup:
	@echo "Setting up pre-commit hooks..."
	@echo "consider running <pre-commit autoupdate> to get the latest versions"
	pre-commit install
	pre-commit install --install-hooks
	pre-commit run --all-files

# Build targets
build: build-debug build-release
	@echo "✓ Build complete"

build-debug:
	@echo "Building debug APK..."
	cd poopyfeed && $(GRADLE) assembleDebug -x test -x lintDebug

build-release:
	@echo "Building release APK..."
	cd poopyfeed && $(GRADLE) assembleRelease -x test -x lintRelease

# Installation targets
install: build-debug
	@echo "Installing debug APK..."
	adb install -r $(APK_DEBUG)

install-release: build-release
	@echo "Installing release APK..."
	adb install -r $(APK_RELEASE)

run: install
	@echo "✓ App installed and ready to run"
	$(MAKE) start

start:
	@echo "Starting app..."
	adb shell am start -n $(PACKAGE_NAME)/$(ACTIVITY)

stop:
	@echo "Stopping app..."
	adb shell am force-stop $(PACKAGE_NAME)

uninstall:
	@echo "Uninstalling app..."
	adb uninstall $(PACKAGE_NAME)

# Cleaning
clean:
	@echo "Cleaning build artifacts..."
	cd poopyfeed && $(GRADLE) clean

# Testing
test:
	@echo "Running unit tests..."
	cd poopyfeed && $(GRADLE) testDebugUnitTest koverVerifyDebug

test-e2e:
	@echo "Running E2E tests on connected device/emulator..."
	cd poopyfeed && $(GRADLE) connectedDebugAndroidTest

# Run E2E critical-path tests (login → child detail → feedings/diapers/naps).
# Requires backend; set E2E_TEST_EMAIL and E2E_TEST_PASSWORD (env or -e for adb).
# Example: E2E_TEST_EMAIL=e2e@test.com E2E_TEST_PASSWORD=secret make test-e2e-critical
test-e2e-critical:
	@echo "Running E2E critical-path tests (backend + credentials required)..."
	cd poopyfeed && $(GRADLE) connectedDebugAndroidTest \
		-PE2E_TEST_EMAIL="$${E2E_TEST_EMAIL}" \
		-PE2E_TEST_PASSWORD="$${E2E_TEST_PASSWORD}" \
		--tests "net.poopyfeed.pf.CriticalPathE2eTest"

coverage:
	@echo "Running unit tests with Kover coverage..."
	cd poopyfeed && $(GRADLE) koverHtmlReportDebug
	@echo ""
	@echo "Kover coverage (LINE, 85%% minimum):"
	@if [ -f "poopyfeed/app/build/reports/kover/html/debug/index.html" ]; then \
		echo "  HTML report: poopyfeed/app/build/reports/kover/html/debug/index.html"; \
		(xdg-open poopyfeed/app/build/reports/kover/html/debug/index.html 2>/dev/null || open poopyfeed/app/build/reports/kover/html/debug/index.html 2>/dev/null) || true; \
	else \
		echo "  Report not found (run 'make coverage' from repo root)."; \
	fi

# Lint
lint:
	@echo "Running lint checks..."
	cd poopyfeed && $(GRADLE) lint

lint-fix:
	@echo "Attempting to fix lint issues..."
	cd poopyfeed && $(GRADLE) updateLintBaseline

# Device management
devices:
	@echo "Connected devices/emulators:"
	adb devices

logs:
	@echo "Showing logcat (press Ctrl+C to stop)..."
	adb logcat -s $(PACKAGE_NAME)

logs-all:
	@echo "Showing all logcat output (press Ctrl+C to stop)..."
	adb logcat

shell:
	@echo "Opening adb shell..."
	adb shell

# Development
format:
	@echo "Formatting Kotlin code..."
	cd poopyfeed && $(GRADLE) spotlessApply

# APK info
apk-info:
	@echo "Debug APK Info:"
	@ls -lh $(APK_DEBUG) 2>/dev/null || echo "  Not found - run 'make build-debug'"
	@echo ""
	@echo "Release APK Info:"
	@ls -lh $(APK_RELEASE) 2>/dev/null || echo "  Not found - run 'make build-release'"

# Gradle wrapper status
gradle-version:
	cd poopyfeed && $(GRADLE) --version
