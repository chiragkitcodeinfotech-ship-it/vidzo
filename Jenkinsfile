// Builds the two test APKs and puts them in the web folder so they can be downloaded:
//   vidzo.apk                 → normal users (Cricbuzz tile)
//   vidzo-<campaign>.apk      → marketing-link users (campaign tile, code must exist in Campaigns.kt)
// Server setup: see README.md → "Jenkins".
pipeline {
    agent any

    parameters {
        string(name: 'LINK_CAMPAIGN', defaultValue: 'offer1',
               description: 'Campaign code for the marketing-link APK (must be listed in Campaigns.kt)')
    }

    environment {
        // Build runs inside this Docker image (has JDK 21; the Android SDK is
        // installed into it on first run and cached in the android-sdk-cache
        // Docker volume). Change APK_DIR if your server serves apks from
        // a different folder.
        APK_DIR     = '/var/www/html/apk'
        BUILD_IMAGE = 'eclipse-temurin:21-jdk-jammy'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Unit tests') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$WORKSPACE":/workspace \
                        -v android-sdk-cache:/opt/android-sdk \
                        -v gradle-cache:/root/.gradle \
                        -w /workspace \
                        "$BUILD_IMAGE" \
                        bash ci/build-in-docker.sh testDebugUnitTest
                '''
            }
        }
        stage('Normal APK') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$WORKSPACE":/workspace \
                        -v android-sdk-cache:/opt/android-sdk \
                        -v gradle-cache:/root/.gradle \
                        -w /workspace \
                        "$BUILD_IMAGE" \
                        bash ci/build-in-docker.sh assembleDebug
                    mkdir -p dist
                    cp app/build/outputs/apk/debug/app-debug.apk dist/vidzo.apk
                '''
            }
        }
        stage('Campaign APK') {
            steps {
                // Gradle rejects anything that is not a short code like offer1.
                sh '''
                    docker run --rm \
                        -v "$WORKSPACE":/workspace \
                        -v android-sdk-cache:/opt/android-sdk \
                        -v gradle-cache:/root/.gradle \
                        -w /workspace \
                        -e LINK_CAMPAIGN="$LINK_CAMPAIGN" \
                        "$BUILD_IMAGE" \
                        bash -c 'bash ci/build-in-docker.sh assembleDebug -Pvidzo.linkCampaign="$LINK_CAMPAIGN"'
                    cp app/build/outputs/apk/debug/app-debug.apk "dist/vidzo-$LINK_CAMPAIGN.apk"
                '''
            }
        }
        stage('Publish') {
            steps {
                sh 'mkdir -p "$APK_DIR" && cp dist/*.apk "$APK_DIR"/'
            }
        }
    }

    post {
        always {
            junit testResults: 'app/build/test-results/**/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'dist/*.apk', allowEmptyArchive: true, fingerprint: true
        }
    }
}
