FROM docker.io/eclipse-temurin:21-jdk

ARG ANDROID_SDK_VERSION=11076708
ARG ANDROID_BUILD_TOOLS_VERSION=35.0.0
ARG ANDROID_PLATFORM_VERSION=35

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Install required system packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends unzip wget && \
    rm -rf /var/lib/apt/lists/*

# Download and install Android SDK command-line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip" -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager \
    "platform-tools" \
    "platforms;android-${ANDROID_PLATFORM_VERSION}" \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"

WORKDIR /project

# Copy Gradle wrapper and build config first for better layer caching
COPY gradlew gradlew
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY gradle.properties gradle.properties
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY app/build.gradle.kts app/build.gradle.kts

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY app/ app/

ENTRYPOINT ["./gradlew"]
CMD ["assembleDebug", "--no-daemon"]
