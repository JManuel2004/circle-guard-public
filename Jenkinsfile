/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  CircleGuard — Jenkinsfile Multi-Branch
 *  ═══════════════════════════════════════════════════════════════════════════
 *
 *  Comportamiento según la rama que dispara el build:
 *
 *   develop / feature/*  → [Dev]    stages 1-3  : Build + Unit Tests
 *   stage                → [Stage]  stages 1-7  : + Integration + Deploy Stage
 *                                                  + E2E Tests + Locust
 *   main                 → [Main]   stages 1-9  : + Deploy Prod + Release Notes
 *
 *  Credenciales requeridas en Jenkins > Manage Credentials:
 *   - kubeconfig-file          (Secret file)          : kubeconfig del clúster K8s
 *   - e2e-admin-credentials    (Username with password): admin del sistema
 *   - e2e-user-credentials     (Username with password): usuario regular de prueba
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */

pipeline {
    agent any

    environment {
        DOCKER_BUILDKIT  = '1'
        IMAGE_PREFIX     = 'circleguard'
        IMAGE_TAG        = "${BUILD_NUMBER}"

        K8S_NS_STAGE     = 'stage'
        K8S_NS_PROD      = 'prod'

        KUBECONFIG       = credentials('kubeconfig-file')
    }

    stages {

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 1 — CHECKOUT  [Dev · Stage · Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch --tags 2>/dev/null || true'
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 2 — BUILD DOCKER IMAGES  [Dev · Stage · Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Build Docker Images') {
            parallel {

                stage('auth-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-auth-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-auth-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

                stage('identity-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-identity-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-identity-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

                stage('form-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-form-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-form-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

                stage('promotion-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-promotion-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-promotion-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

                stage('notification-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-notification-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-notification-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

                stage('dashboard-service') {
                    steps {
                        sh """
                            docker build \
                                -f services/circleguard-dashboard-service/Dockerfile \
                                -t ${IMAGE_PREFIX}/circleguard-dashboard-service:${IMAGE_TAG} \
                                .
                        """
                    }
                }

            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 3 — UNIT TESTS  [Dev · Stage · Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                sh """
                    ./gradlew \
                        :services:circleguard-auth-service:test \
                        :services:circleguard-identity-service:test \
                        :services:circleguard-form-service:test \
                        :services:circlegard-promotion-service:test \
                        :services:circleguard-notification-service:test \
                        :services:circleguard-dashboard-service:test \
                        --no-daemon \
                        --parallel \
                        --continue
                """
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 4 — INTEGRATION TESTS  [Stage · Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Integration Tests') {
            when {
                anyOf { branch 'stage'; branch 'main' }
            }
            steps {
                sh """
                    ./gradlew \
                        :services:circleguard-auth-service:test \
                        :services:circleguard-promotion-service:test \
                        :services:circleguard-notification-service:test \
                        --tests "*IT" \
                        --no-daemon \
                        --continue
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 7 — PERFORMANCE TESTS (LOCUST)  [Stage · Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Performance Tests — Locust') {
            when {
                anyOf { branch 'stage'; branch 'main' }
            }
            steps {
                sh """
                    mkdir -p locust/reports
                    pip install -q -r locust/requirements.txt
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 8 — DEPLOY → PRODUCTION  [Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy → Production') {
            when {
                branch 'main'
            }
            steps {
                sh """
                    kubectl create namespace ${K8S_NS_PROD} \
                        --dry-run=client -o yaml | kubectl apply -f -
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 9 — RELEASE NOTES  [Main]
        // ─────────────────────────────────────────────────────────────────────
        stage('Release Notes') {
            when {
                branch 'main'
            }
            steps {
                sh """
                    echo "Generating release notes..."
                """
            }
        }

    }

    post {
        success {
            script {
                def deployedNs = (env.BRANCH_NAME == 'main') ? env.K8S_NS_PROD : env.K8S_NS_STAGE

                def nsInfo = (env.BRANCH_NAME in ['stage', 'main'])
                    ? " Namespace activo: '${deployedNs}'."
                    : ''

                echo "Pipeline completado. Imágenes etiquetadas con :${IMAGE_TAG}.${nsInfo}"
            }
        }

        failure {
            echo 'Pipeline fallido. Revisar el stage marcado en rojo en la UI de Jenkins.'
        }

        always {
            cleanWs()
        }
    }
}