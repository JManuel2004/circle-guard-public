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
 *   master               → [Master] stages 1-9  : + Deploy Prod + Release Notes
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
        // DOCKER_BUILDKIT=1 habilita el builder moderno (BuildKit) para caché
        // de capas más eficiente y builds paralelos dentro del Dockerfile.
        DOCKER_BUILDKIT  = '1'

        IMAGE_PREFIX     = 'circleguard'

        // IMAGE_TAG usa el número de build de Jenkins, garantizando que cada
        // ejecución produce una imagen con un tag único e irrepetible.
        // Los manifiestos K8s usan ':latest' en disco; sed los parchea en vuelo.
        IMAGE_TAG        = "${BUILD_NUMBER}"

        // Namespaces de Kubernetes por entorno.
        K8S_NS_STAGE     = 'stage'
        K8S_NS_PROD      = 'prod'

        // KUBECONFIG: Jenkins crea un archivo temporal con el contenido del
        // Secret 'kubeconfig-file' y pone su ruta en esta variable.
        // kubectl y helm la detectan automáticamente.
        KUBECONFIG       = credentials('kubeconfig-file')
    }

    stages {

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 1 — CHECKOUT  [Dev · Stage · Master]
        // ─────────────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm

                // Traer los tags de Git para que Release Notes pueda calcular
                // el delta de commits desde el último tag.
                sh 'git fetch --tags 2>/dev/null || true'
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 2 — BUILD DOCKER IMAGES  [Dev · Stage · Master]
        //
        // Regla de oro del taller: el contexto de docker build es SIEMPRE
        // la raíz del repositorio (.) para que el stage builder acceda a
        // gradlew, gradle/, settings.gradle.kts y build.gradle.kts.
        //
        // Los 6 servicios se construyen en PARALELO para reducir el tiempo
        // total de la etapa.
        // ─────────────────────────────────────────────────────────────────────
        stage('Build Docker Images') {
            parallel {

                stage('auth-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-auth-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-auth-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

                stage('identity-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-identity-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-identity-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

                stage('form-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-form-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-form-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

                stage('promotion-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-promotion-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-promotion-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

                stage('notification-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-notification-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-notification-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

                stage('dashboard-service') {
                    steps {
                        sh """
                            docker build \\
                                -f services/circleguard-dashboard-service/Dockerfile \\
                                -t ${IMAGE_PREFIX}/circleguard-dashboard-service:${IMAGE_TAG} \\
                                .
                        """
                    }
                }

            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 3 — UNIT TESTS  [Dev · Stage · Master]
        //
        // --parallel  : los módulos Gradle se ejecutan en paralelo.
        // --continue  : si un módulo falla, los demás continúan; Jenkins
        //               marcará el build como fallido al final con el resumen
        //               completo, en lugar de abortar al primer error.
        // junit '**'  : recolecta todos los XML de Surefire/JUnit 5 para que
        //               Jenkins muestre el reporte de pruebas en la UI.
        // ─────────────────────────────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                sh """
                    ./gradlew \\
                        :services:circleguard-auth-service:test \\
                        :services:circleguard-identity-service:test \\
                        :services:circleguard-form-service:test \\
                        :services:circleguard-promotion-service:test \\
                        :services:circleguard-notification-service:test \\
                        :services:circleguard-dashboard-service:test \\
                        --no-daemon \\
                        --parallel \\
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
        // STAGE 4 — INTEGRATION TESTS  [Stage · Master]
        //
        // Filtra por sufijo IT (convención de nomenclatura del proyecto):
        //   AuthToIdentityHttpIT         → auth-service    (WireMock)
        //   SurveyToSuspectKafkaIT       → promotion-service (EmbeddedKafka)
        //   CertificateApprovedToActiveIT→ promotion-service (EmbeddedKafka)
        //   PriorityAlertToAdminEmailIT  → notification-service (WireMock + Kafka)
        //   CircleFencedRoomCancellationIT→notification-service (EmbeddedKafka)
        //
        // NO requieren servicios K8s desplegados: toda la infraestructura
        // (Kafka, HTTP externo) es in-process gracias a EmbeddedKafka y WireMock.
        //
        // El flag --tests "*IT" al final del comando Gradle se aplica a TODOS
        // los módulos listados (comportamiento de Gradle >= 5.0).
        // ─────────────────────────────────────────────────────────────────────
        stage('Integration Tests') {
            when {
                anyOf { branch 'stage'; branch 'main' }
            }
            steps {
                sh """
                    ./gradlew \\
                        :services:circleguard-auth-service:test \\
                        :services:circleguard-promotion-service:test \\
                        :services:circleguard-notification-service:test \\
                        --tests "*IT" \\
                        --no-daemon \\
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
        // STAGE 5 — DEPLOY → NAMESPACE STAGE  [Stage · Master]
        //
        // El patrón `--dry-run=client -o yaml | kubectl apply -f -` crea el
        // namespace si no existe y no falla si ya existe (idempotente).
        //
        // sed reemplaza ':latest' por el IMAGE_TAG real en cada manifest
        // antes de enviarlo a kubectl; los archivos en disco no se modifican.
        //
        // `kubectl rollout status --timeout=180s` bloquea el pipeline hasta
        // que cada Deployment tenga todos sus Pods en estado Running/Ready.
        // Si un Pod no arranca en 3 minutos, el stage falla con mensaje claro.
        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy → Stage Namespace') {
            when {
                anyOf { branch 'stage'; branch 'master' }
            }
            steps {
                // Crear namespace de forma idempotente
                sh """
                    kubectl create namespace ${K8S_NS_STAGE} \\
                        --dry-run=client -o yaml | kubectl apply -f -
                """

                // Aplicar los 6 manifiestos inyectando el tag de imagen real
                sh """
                    for f in k8s/*.yml; do
                        sed "s|:latest|:${IMAGE_TAG}|g" "\$f" \\
                            | kubectl apply -n ${K8S_NS_STAGE} -f -
                    done
                """

                // Esperar a que todos los Deployments estén 100 % disponibles
                sh """
                    for svc in \\
                        circleguard-auth-service \\
                        circleguard-identity-service \\
                        circleguard-form-service \\
                        circleguard-promotion-service \\
                        circleguard-notification-service \\
                        circleguard-dashboard-service; \\
                    do
                        echo "Esperando rollout de \$svc en namespace ${K8S_NS_STAGE}..."
                        kubectl rollout status deployment/\$svc \\
                            -n ${K8S_NS_STAGE} \\
                            --timeout=180s
                    done
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 6 — E2E TESTS  [Stage · Master]
        //
        // Los tests REST Assured (módulo e2e-tests/) son black-box: golpean
        // los servicios reales desplegados en Stage.  Como Jenkins corre fuera
        // del clúster, se usan port-forwards para exponer los ClusterIP en
        // puertos locales del agente Jenkins.
        //
        // Los port-forwards se lanzan en background (&) y se termina en
        // post { always } vía pkill, incluso si los tests fallan.
        //
        // Las credenciales E2E se leen de Jenkins Credentials (nunca en claro
        // en el Jenkinsfile) y se exportan como variables de entorno que
        // build.gradle.kts de e2e-tests lee con System.getenv().
        // ─────────────────────────────────────────────────────────────────────
        stage('E2E Tests') {
            when {
                anyOf { branch 'stage'; branch 'master' }
            }
            steps {
                // Port-forward: ClusterIP Stage → localhost del agente Jenkins
                sh """
                    kubectl port-forward svc/circleguard-auth-service      8180:8180 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-identity-service   8083:8083 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-form-service       8086:8086 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-promotion-service  8088:8088 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-dashboard-service  8084:8084 \\
                        -n ${K8S_NS_STAGE} &

                    # Breve espera para que los túneles estén listos antes de lanzar tests
                    sleep 5
                """

                // Inyectar credenciales desde Jenkins Credentials Store
                withCredentials([
                    usernamePassword(
                        credentialsId: 'e2e-admin-credentials',
                        usernameVariable: 'E2E_ADMIN_USER',
                        passwordVariable: 'E2E_ADMIN_PASS'
                    ),
                    usernamePassword(
                        credentialsId: 'e2e-user-credentials',
                        usernameVariable: 'E2E_USER',
                        passwordVariable: 'E2E_USER_PASS'
                    )
                ]) {
                    // Las variables de entorno aquí son recogidas por e2e-tests/build.gradle.kts
                    // mediante System.getenv() y pasadas como systemProperty a la JVM de tests.
                    sh """
                        AUTH_SERVICE_URL=http://localhost:8180 \\
                        IDENTITY_SERVICE_URL=http://localhost:8083 \\
                        FORM_SERVICE_URL=http://localhost:8086 \\
                        PROMOTION_SERVICE_URL=http://localhost:8088 \\
                        DASHBOARD_SERVICE_URL=http://localhost:8084 \\
                        ./gradlew :e2e-tests:test --no-daemon
                    """
                }
            }
            post {
                always {
                    // Terminar TODOS los port-forwards antes de continuar
                    sh "pkill -f 'kubectl port-forward' || true"
                    junit 'e2e-tests/build/test-results/test/*.xml'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 7 — PERFORMANCE TESTS (LOCUST)  [Stage · Master]
        //
        // Escenario: 50 usuarios virtuales durante 5 minutos contra Stage.
        // Los port-forwards del stage anterior ya fueron terminados en su
        // post { always }; se restablecen aquí solo para los servicios que
        // el locustfile.py utiliza (auth + form + dashboard).
        //
        // --exit-code-on-error 1: Locust retorna código 1 si hubo cualquier
        // fallo HTTP, lo que hace que el stage Jenkins falle.
        //
        // Los artefactos CSV/HTML se archivan incluso si Locust falla, para
        // que el reporte de métricas esté disponible en la UI de Jenkins.
        //
        // Archivos generados:
        //   locust/reports/results_stats.csv          → percentiles p50/p95/p99 + RPS
        //   locust/reports/results_stats_history.csv  → evolución temporal (cada ~2s)
        //   locust/reports/results_failures.csv       → detalle de errores por endpoint
        //   locust/reports/report.html                → dashboard interactivo
        // ─────────────────────────────────────────────────────────────────────
        stage('Performance Tests — Locust') {
            when {
                anyOf { branch 'stage'; branch 'master' }
            }
            steps {
                // Restablecer port-forwards para los servicios del flujo Locust
                sh """
                    kubectl port-forward svc/circleguard-auth-service      8180:8180 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-form-service       8086:8086 \\
                        -n ${K8S_NS_STAGE} &
                    kubectl port-forward svc/circleguard-dashboard-service  8084:8084 \\
                        -n ${K8S_NS_STAGE} &
                    sleep 5
                """

                sh """
                    mkdir -p locust/reports
                    pip install -q -r locust/requirements.txt

                    AUTH_SERVICE_URL=http://localhost:8180 \\
                    FORM_SERVICE_URL=http://localhost:8086 \\
                    DASHBOARD_SERVICE_URL=http://localhost:8084 \\
                    locust \\
                        -f locust/locustfile.py \\
                        --headless \\
                        --users 50 \\
                        --spawn-rate 5 \\
                        --run-time 5m \\
                        --csv=locust/reports/results \\
                        --html=locust/reports/report.html \\
                        --logfile=locust/reports/locust.log \\
                        --loglevel=INFO \\
                        --exit-code-on-error 1
                """
            }
            post {
                always {
                    sh "pkill -f 'kubectl port-forward' || true"
                    // Archivar CSV y HTML aunque el stage haya fallado, para
                    // que los reportes de métricas estén siempre disponibles.
                    archiveArtifacts(
                        artifacts: 'locust/reports/**',
                        allowEmptyArchive: true
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 8 — DEPLOY → PRODUCTION  [Master]
        //
        // Se ejecuta SOLO en la rama master, después de que E2E y Locust
        // validaron el entorno Stage con el mismo IMAGE_TAG.
        // Lógica idéntica al deploy de Stage; apunta al namespace 'prod'.
        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy → Production') {
            when {
                branch 'master'
            }
            steps {
                sh """
                    kubectl create namespace ${K8S_NS_PROD} \\
                        --dry-run=client -o yaml | kubectl apply -f -
                """

                sh """
                    for f in k8s/*.yml; do
                        sed "s|:latest|:${IMAGE_TAG}|g" "\$f" \\
                            | kubectl apply -n ${K8S_NS_PROD} -f -
                    done
                """

                sh """
                    for svc in \\
                        circleguard-auth-service \\
                        circleguard-identity-service \\
                        circleguard-form-service \\
                        circleguard-promotion-service \\
                        circleguard-notification-service \\
                        circleguard-dashboard-service; \\
                    do
                        echo "Esperando rollout de \$svc en namespace ${K8S_NS_PROD}..."
                        kubectl rollout status deployment/\$svc \\
                            -n ${K8S_NS_PROD} \\
                            --timeout=180s
                    done
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // STAGE 9 — RELEASE NOTES  [Master]
        //
        // Genera RELEASE_NOTES.md extrayendo los commits entre el último
        // tag Git y HEAD.  Si no hay ningún tag previo, usa el commit inicial.
        //
        // Estructura del archivo generado:
        //   • Tabla de metadatos (fecha, build, commit, rama)
        //   • Lista de commits --no-merges (excluye merges de PR)
        //   • Tabla de imágenes Docker desplegadas con su tag exacto
        //
        // El archivo se archiva como artefacto en Jenkins (fingerprinted)
        // para trazabilidad entre builds y deploys.
        //
        // El git tag es opcional (requiere push access configurado en Jenkins).
        // ─────────────────────────────────────────────────────────────────────
        stage('Release Notes') {
            when {
                branch 'master'
            }
            steps {
                // Generar RELEASE_NOTES.md
                // Nota: ${IMAGE_TAG} y ${BUILD_NUMBER} son variables Groovy
                // (interpoladas por el motor de plantillas del pipeline).
                // \$(...) y \${VAR} son variables de shell (escapadas para
                // que Groovy las deje pasar sin interpretar).
                sh """
                    DATE=\$(date "+%Y-%m-%d")
                    COMMIT_SHORT=\$(git rev-parse --short HEAD)
                    LAST_TAG=\$(git describe --tags --abbrev=0 HEAD~1 2>/dev/null \\
                               || git rev-list --max-parents=0 HEAD)

                    {
                        echo "# Release Notes — v${IMAGE_TAG}"
                        echo ""
                        echo "| Campo          | Valor                                     |"
                        echo "|----------------|-------------------------------------------|"
                        echo "| Fecha          | \${DATE}                                 |"
                        echo "| Build Jenkins  | #${BUILD_NUMBER}                         |"
                        echo "| Commit HEAD    | \${COMMIT_SHORT}                         |"
                        echo "| Rama           | ${GIT_BRANCH}                            |"
                        echo ""
                        echo "---"
                        echo ""
                        echo "## Cambios incluidos desde \${LAST_TAG}"
                        echo ""
                        git log \${LAST_TAG}..HEAD \\
                            --no-merges \\
                            --pretty=format:"- **%s**  (%h — %an)" \\
                        || echo "- Sin commits nuevos desde el último tag."
                        echo ""
                        echo ""
                        echo "---"
                        echo ""
                        echo "## Imágenes Docker desplegadas en producción"
                        echo ""
                        echo "| Servicio                          | Tag          | Namespace |"
                        echo "|-----------------------------------|--------------|-----------|"
                        echo "| circleguard-auth-service          | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo "| circleguard-identity-service      | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo "| circleguard-form-service          | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo "| circleguard-promotion-service     | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo "| circleguard-notification-service  | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo "| circleguard-dashboard-service     | ${IMAGE_TAG} | ${K8S_NS_PROD} |"
                        echo ""
                        echo "---"
                        echo "_Generado automáticamente por Jenkins · Build #${BUILD_NUMBER}_"
                    } > RELEASE_NOTES.md

                    echo "=== RELEASE_NOTES.md generado ==="
                    cat RELEASE_NOTES.md
                """

                // Crear tag Git anotado para este release.
                // Requiere que el agente Jenkins tenga acceso de escritura al repo.
                // '|| true' evita que el pipeline falle si el tag ya existe.
                sh """
                    git tag -a "v${IMAGE_TAG}" \\
                        -m "Release v${IMAGE_TAG} — Jenkins Build #${BUILD_NUMBER}" \\
                        2>/dev/null || echo "Tag v${IMAGE_TAG} ya existe; se omite."
                """

                // Archivar el archivo como artefacto del build en Jenkins
                archiveArtifacts(
                    artifacts: 'RELEASE_NOTES.md',
                    fingerprint: true
                )
            }
        }

    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST-BUILD
    // ─────────────────────────────────────────────────────────────────────────
    post {
        success {
            script {
                def deployedNs = (env.BRANCH_NAME == 'master') ? env.K8S_NS_PROD : env.K8S_NS_STAGE
                def nsInfo     = (env.BRANCH_NAME in ['stage', 'master'])
                                    ? " Namespace activo: '${deployedNs}'."
                                    : ''
                echo "Pipeline completado. Imágenes etiquetadas con :${IMAGE_TAG}.${nsInfo}"
            }
        }
        failure {
            echo 'Pipeline fallido. Revisar el stage marcado en rojo en la UI de Jenkins.'
        }
        always {
            // cleanWs() elimina el workspace del agente al final del build,
            // evitando acumulación de artefactos entre builds.
            cleanWs()
        }
    }
}
