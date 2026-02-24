RUNTIME := podman

.PHONY: help
help:
	@echo "PoopyFeed Android Commands"
	@echo "=========================="
	@echo "make build          - Build debug APK in container"
	@echo "make test           - Run unit tests in container"
	@echo "make test-coverage  - Run tests with JaCoCo coverage (for SonarQube)"
	@echo "make lint           - Run lint checks in container"
	@echo "make format         - Format Kotlin code"
	@echo "make clean          - Clean build artifacts in container"
	@echo ""
	@echo "Development Setup:"
	@echo "make pre-commit-setup - Install pre-commit hooks"
	@echo ""
	@echo "Note: Run from root directory for compose integration:"
	@echo "  make build-android / make test-android / make lint-android / make format-android"

.PHONY: build
build:
	@echo "Building debug APK..."
	cd .. && $(RUNTIME) compose --profile android build android
	cd .. && $(RUNTIME) compose --profile android run --rm android assembleDebug --no-daemon

.PHONY: test
test:
	@echo "Running unit tests..."
	cd .. && $(RUNTIME) compose --profile android build android
	cd .. && $(RUNTIME) compose --profile android run --rm android test --no-daemon

.PHONY: test-coverage
test-coverage:
	@echo "Running unit tests with coverage..."
	cd .. && $(RUNTIME) compose --profile android build android
	cd .. && $(RUNTIME) compose --profile android run --rm android jacocoTestReport --no-daemon

.PHONY: lint
lint:
	@echo "Running lint checks..."
	cd .. && $(RUNTIME) compose --profile android build android
	cd .. && $(RUNTIME) compose --profile android run --rm android lint --no-daemon

.PHONY: format
format:
	@echo "Formatting Kotlin code..."
	@if grep -q "spotless\|ktlint" build.gradle.kts app/build.gradle.kts 2>/dev/null; then \
		./gradlew spotlessApply --no-daemon 2>/dev/null || ./gradlew ktlintFormat --no-daemon; \
	else \
		echo "⚠️  No formatting plugin detected. Install ktlint or Spotless plugin first."; \
		echo "   See: https://github.com/JLLeitschuh/ktlint-gradle or https://github.com/diffplug/spotless"; \
	fi

.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	cd .. && $(RUNTIME) compose --profile android run --rm android clean --no-daemon

.PHONY: pre-commit-setup
pre-commit-setup:
	@echo "Installing pre-commit hooks..."
	pre-commit install
	pre-commit install --install-hooks
	@echo "Pre-commit hooks installed!"
