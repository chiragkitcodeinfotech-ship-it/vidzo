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
        // Change these three paths if your server uses different folders.
        JAVA_HOME    = '/usr/lib/jvm/java-21-openjdk-amd64'
        ANDROID_HOME = '/opt/android-sdk'
        APK_DIR      = '/var/www/html/apk'
        PATH         = "${JAVA_HOME}/bin:${PATH}"
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Unit tests') {
            steps {
                sh 'chmod +x gradlew && ./gradlew --no-daemon testDebugUnitTest'
            }
        }
        stage('Normal APK') {
            steps {
                sh '''
                    ./gradlew --no-daemon assembleDebug
                    mkdir -p dist
                    cp app/build/outputs/apk/debug/app-debug.apk dist/vidzo.apk
                '''
            }
        }
        stage('Campaign APK') {
            steps {
                // Gradle rejects anything that is not a short code like offer1.
                sh '''
                    ./gradlew --no-daemon assembleDebug -Pvidzo.linkCampaign="$LINK_CAMPAIGN"
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
