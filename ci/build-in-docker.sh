#!/usr/bin/env bash
# Runs inside the eclipse-temurin:21-jdk Docker container (see Jenkinsfile).
# Installs the Android SDK into /opt/android-sdk once (cached across builds via
# a named Docker volume), then runs the requested Gradle task(s).
set -euo pipefail

export ANDROID_HOME=/opt/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    apt-get update -qq
    apt-get install -y -qq unzip wget >/dev/null
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
    unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
    sdkmanager "platforms;android-36" "build-tools;36.0.0" >/dev/null
fi

chmod +x ./gradlew
./gradlew --no-daemon "$@"
