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
        // Change this if your server serves apks from a different folder.
        APK_DIR = '/var/www/html/apk'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        // Builds with `docker build` (ci/Dockerfile), not `docker run -v $WORKSPACE:...`:
        // Jenkins itself runs in a container here, so a bind-mount path like
        // $WORKSPACE only resolves on the real Docker host, not Jenkins' own
        // filesystem, and silently mounts an empty directory. `docker build`
        // sends the build context over the Docker API instead, which works
        // regardless of that — it's the same reason the other two projects on
        // this server (inoutbilling_frontend/backend) use `docker build`/
        // `docker-compose` rather than `docker run -v`.
        stage('Build & test (Docker)') {
            steps {
                sh '''
                    DOCKER_BUILDKIT=1 docker build \
                        --build-arg LINK_CAMPAIGN="$LINK_CAMPAIGN" \
                        -t "vidzo-build:$BUILD_NUMBER" \
                        -f ci/Dockerfile .
                '''
            }
        }
        stage('Extract APKs') {
            steps {
                sh '''
                    CID=$(docker create "vidzo-build:$BUILD_NUMBER")
                    mkdir -p dist app/build
                    docker cp "$CID":/workspace/dist/. dist/
                    docker cp "$CID":/workspace/app/build/test-results app/build/test-results 2>/dev/null || true
                    docker rm "$CID"
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
            sh 'docker rmi "vidzo-build:$BUILD_NUMBER" || true'
        }
    }
}
