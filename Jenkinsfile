// Builds the two test APKs and serves them for download via a small nginx
// container on this server (http://<server>:8091/<file>.apk by default):
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
        // The APKs are served by a small dedicated nginx container (started
        // below if missing) on this host port — not /var/www/html, which
        // doesn't work here: Jenkins itself runs in a container, so a plain
        // `cp` in a Jenkinsfile `sh` step only writes inside Jenkins' own
        // filesystem, never onto the real server. Change the port if 8091
        // is already used by something else on your server.
        APK_SERVER_NAME = 'vidzo-apk-server'
        APK_SERVER_PORT = '8091'
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
                    docker build \
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
                sh '''
                    if ! docker ps -a --format "{{.Names}}" | grep -qx "$APK_SERVER_NAME"; then
                        docker run -d --name "$APK_SERVER_NAME" \
                            -p "$APK_SERVER_PORT":80 \
                            -v vidzo-apk-dist:/usr/share/nginx/html \
                            --restart unless-stopped \
                            nginx:alpine
                    else
                        docker start "$APK_SERVER_NAME" >/dev/null 2>&1 || true
                    fi
                    docker cp dist/. "$APK_SERVER_NAME":/usr/share/nginx/html/
                '''
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
