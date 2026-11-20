# syntax=docker/dockerfile:1
# Builds the app and runs its unit tests. Used by ../Jenkinsfile via `docker build`
# (not `docker run -v`, which breaks when Jenkins itself runs in a container —
# see Jenkinsfile for why).
FROM eclipse-temurin:21-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

RUN apt-get update -qq \
    && apt-get install -y -qq unzip wget \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools" \
    && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses >/dev/null 2>&1 || true \
    && sdkmanager "platforms;android-36" "build-tools;36.0.0" >/dev/null

WORKDIR /workspace
COPY . .

ARG LINK_CAMPAIGN=offer1
# The cache mount keeps Gradle/Android dependencies across builds without
# baking them into the image layer (unaffected by the same bind-mount issue
# that broke `docker run -v` — this is Docker's own build cache, not a host path).
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew \
    && ./gradlew --no-daemon testDebugUnitTest assembleDebug \
    && mkdir -p dist \
    && cp app/build/outputs/apk/debug/app-debug.apk dist/vidzo.apk \
    && ./gradlew --no-daemon assembleDebug -Pvidzo.linkCampaign="$LINK_CAMPAIGN" \
    && cp app/build/outputs/apk/debug/app-debug.apk "dist/vidzo-${LINK_CAMPAIGN}.apk"
