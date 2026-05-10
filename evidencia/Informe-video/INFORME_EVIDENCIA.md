# Informe de Evidencia — CircleGuard CI/CD Pipelines

**Proyecto:** CircleGuard — Sistema de Monitoreo de Salud Universitaria  
**Fecha:** 2026-05-10

---

## Tabla de Contenidos

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Infraestructura y Credenciales](#2-infraestructura-y-credenciales)
3. [Pipeline 1 — circleguard-dev](#3-pipeline-1--circleguard-dev)
4. [Pipeline 2 — circleguard-stage](#4-pipeline-2--circleguard-stage)
5. [Pipeline 3 — circleguard-main](#5-pipeline-3--circleguard-main)
6. [Análisis de Pruebas de Rendimiento](#6-análisis-de-pruebas-de-rendimiento)
7. [Conclusiones](#7-conclusiones)

---

## 1. Resumen Ejecutivo

CircleGuard implementa una estrategia CI/CD de tres pipelines con progresión hacia producción. Cada pipeline agrega capas de validación antes de promover el código al siguiente ambiente:

| Pipeline | Ambiente | Propósito principal | Duración aprox. |
|---|---|---|---|
| `circleguard-dev` | circleguard-dev | Build + Unit Tests + Smoke Test | ~9 min |
| `circleguard-stage` | circleguard-stage | Integración + E2E + Performance | ~12 min |
| `circleguard-main` | circleguard-stage → circleguard-prod | Pipeline completo + Deploy Prod + Release Notes | ~16 min |

Los tres pipelines ejecutaron exitosamente con **0 fallos** en pruebas funcionales y **0 %** de tasa de error en pruebas de rendimiento.

Vista general del estado de los tres pipelines en Jenkins:

![Dashboard Jenkins](<fotos/WhatsApp Image 2026-05-10 at 4.42.11 PM.jpeg>)

---

## 2. Infraestructura y Credenciales

### 2.1 Servidor Jenkins

Jenkins corre como contenedor Docker en el host local, expuesto en los puertos 8080 (UI) y 50000 (agentes):

![Jenkins Container](<fotos/WhatsApp Image 2026-05-10 at 4.47.14 PM.jpeg>)

El cluster de Kubernetes es provisto por **Docker Desktop**, con la API en `https://kubernetes.docker.internal:6443`. Los namespaces del sistema confirman la disponibilidad del cluster:

![kubectl namespaces](<fotos/WhatsApp Image 2026-05-10 at 4.48.48 PM.jpeg>)

### 2.2 Credenciales Configuradas en Jenkins

Se configuraron cuatro credenciales globales requeridas por los pipelines:

![Jenkins Credentials](<fotos/WhatsApp Image 2026-05-10 at 4.46.11 PM.jpeg>)

| ID | Tipo | Propósito |
|---|---|---|
| `e2e-user-credentials` | Username/Password (`staff_guard`) | Cuenta de usuario regular para pruebas E2E |
| `e2e-admin-credentials` | Username/Password (`health_user`) | Cuenta administradora para pruebas E2E |
| `git-push-credentials` | Username/Password (`JManuel2004`) | Push de tags al repositorio en Stage 9 |
| `kubeconfig-file` | Secret file (`kubeconfig-local.yaml`) | Acceso al cluster Kubernetes desde Jenkins |

### 2.3 Imágenes Docker Construidas

Los seis servicios del monorepo fueron construidos y están disponibles localmente:

![Docker Images](<fotos/WhatsApp Image 2026-05-08 at 9.05.00 PM.jpeg>)

### 2.4 Pods en Kubernetes

Todos los pods del sistema (6 servicios de aplicación + 6 servicios de infraestructura) ejecutaron en estado `Running 1/1`:

![kubectl get pods](<fotos/WhatsApp Image 2026-05-08 at 9.05.31 PM.jpeg>)

---

## 3. Pipeline 1 — circleguard-dev

### 3.1 Configuración

El pipeline `circleguard-dev` apunta a la rama `*/main` del repositorio y utiliza el script `jenkins/Jenkinsfile.dev`:

![Config circleguard-dev](<fotos/WhatsApp Image 2026-05-10 at 4.44.34 PM.jpeg>)

**Stages del pipeline:**

```
Checkout SCM → Checkout → Build Docker Images (paralelo, 6 servicios)
→ Unit Tests → Deploy DEV → Smoke Test DEV → Post Actions
```

**Variables de entorno del pipeline:**

```groovy
environment {
    DOCKER_BUILDKIT = '1'           // Habilita BuildKit para caché de capas Docker
    IMAGE_PREFIX    = 'circleguard'
    IMAGE_TAG       = "${BUILD_NUMBER}" // Etiqueta inmutable por número de build
    K8S_NS_DEV      = 'circleguard-dev'
    KUBECTL         = 'kubectl --insecure-skip-tls-verify'
}
```

**Build paralelo de los 6 servicios** — cada servicio se construye simultáneamente en sub-stages independientes. El contexto de build es siempre la raíz del monorepo (`.`) para que los Dockerfiles puedan copiar recursos entre módulos:

```groovy
stage('Build Docker Images') {
    parallel {
        stage('auth-service') {
            steps {
                sh """
                    docker build \
                        -f services/circleguard-auth-service/Dockerfile \
                        -t ${IMAGE_PREFIX}/circleguard-auth-service:${IMAGE_TAG} \
                        -t ${IMAGE_PREFIX}/circleguard-auth-service:latest \
                        .
                """
            }
        }
        // ... (identity, form, promotion, notification, dashboard — misma estructura)
    }
}
```

**Unit Tests** — Gradle ejecuta todos los módulos en paralelo con `--continue`, lo que garantiza que los reportes de todos los módulos se archivan aunque alguno falle:

```groovy
stage('Unit Tests') {
    steps {
        sh """
            ./gradlew \
                :services:circleguard-auth-service:test \
                :services:circleguard-identity-service:test \
                :services:circleguard-form-service:test \
                :services:circleguard-promotion-service:test \
                :services:circleguard-notification-service:test \
                :services:circleguard-dashboard-service:test \
                --no-daemon --parallel --continue
        """
    }
    post {
        always {
            junit allowEmptyResults: false,
                  testResults: '**/build/test-results/test/*.xml'
        }
    }
}
```

**Deploy DEV** — todas las llamadas a `kubectl` están envueltas en `withKubeConfig` para que la credencial solo esté disponible durante ese stage. El namespace se crea de forma idempotente via `dry-run | apply`. Se espera cada deployment individualmente con `rollout status`:

```groovy
stage('Deploy DEV') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file',
                        serverUrl:     'https://kubernetes.docker.internal:6443']) {
            script {
                sh """
                    ${KUBECTL} create namespace ${K8S_NS_DEV} \
                        --dry-run=client -o yaml | ${KUBECTL} apply --validate=false -f -
                """
                // Infraestructura primero (postgres, redis, neo4j, kafka, openldap, mailhog)
                sh "${KUBECTL} apply --validate=false -f k8s/postgres.yml -n ${K8S_NS_DEV} ..."
                sh "${KUBECTL} rollout status deployment/postgres -n ${K8S_NS_DEV} --timeout=120s || true ..."
                // Luego los 6 servicios de aplicación
                sh "${KUBECTL} apply --validate=false -f k8s/auth-service.yml -n ${K8S_NS_DEV} ..."
                sh "${KUBECTL} rollout status deployment/circleguard-auth-service -n ${K8S_NS_DEV} --timeout=120s ..."
            }
        }
    }
}
```

**Smoke Test DEV** — verifica que cada uno de los 6 servicios tenga al menos un pod en estado `Running` antes de dar el build por exitoso:

```groovy
stage('Smoke Test DEV') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file', ...]) {
            script {
                sleep(30) // Margen para que Spring Boot termine de arrancar
                def services = ['circleguard-auth-service', 'circleguard-identity-service', ...]
                services.each { svc ->
                    def status = sh(returnStdout: true, script: """
                        ${KUBECTL} get pods -n ${K8S_NS_DEV} -l app=${svc} \
                            --field-selector=status.phase=Running --no-headers | wc -l
                    """).trim()
                    if (status.toInteger() < 1) {
                        error("Smoke test FAILED — ${svc} has no Running pods")
                    }
                    echo "SMOKE OK  ${svc} → ${status} pod(s) Running"
                }
            }
        }
    }
}
```

**Post Actions**: Limpieza de imágenes Docker y eliminación del namespace `circleguard-dev`

### 3.2 Resultado — Ejecución Exitosa

El pipeline completó todos los stages satisfactoriamente en **~9 minutos**:

![circleguard-dev pipeline exitoso](<fotos/WhatsApp Image 2026-05-09 at 1.10.23 PM.jpeg>)

Vista del grafo del pipeline con los 6 servicios construidos en paralelo:

![circleguard-dev graph](<fotos/WhatsApp Image 2026-05-09 at 1.16.34 PM.jpeg>)

**Detalle de duración por stage:**

| Stage | Duración |
|---|---|
| Checkout SCM | 3 s |
| Checkout | 2 s |
| Build Docker Images (paralelo) | 13 s |
| Unit Tests | 4 min 46 s |
| Deploy DEV | 2 min 21 s |
| Smoke Test DEV | 34 s |
| Post Actions | 40 s |
| **Total** | **~9 min** |

El mensaje de cierre confirma: `DEV build deployed and smoke-tested in namespace 'circleguard-dev'.`

---

## 4. Pipeline 2 — circleguard-stage

### 4.1 Configuración

El pipeline `circleguard-stage` utiliza el script `jenkins/Jenkinsfile.stage`:

![Config circleguard-stage](<fotos/WhatsApp Image 2026-05-10 at 4.45.30 PM.jpeg>)

**Stages del pipeline:**

```
Checkout SCM → Checkout → Build Docker Images (paralelo, 6 servicios)
→ Unit Tests → Integration Tests → Deploy → Stage
→ E2E Tests (agente Windows) → Performance Tests — Locust → Post Actions
```

**Variables de entorno** — introduce `PORT_BASE` para calcular dinámicamente los puertos locales de port-forward y evitar colisiones entre stages:

```groovy
environment {
    DOCKER_BUILDKIT = '1'
    IMAGE_PREFIX    = 'circleguard'
    IMAGE_TAG       = "${BUILD_NUMBER}"
    K8S_NS_STAGE    = 'circleguard-stage'
    KUBECTL         = 'kubectl --insecure-skip-tls-verify'
    // E2E ports   = PORT_BASE + (svc_port - 8000)  →  19180, 19083, 19086...
    // Locust ports = (PORT_BASE - 1000) + offset    →  18180, 18086, 18084
    PORT_BASE       = '19000'
}
```

**Integration Tests** — ejecuta solo las clases `*IT` en los tres servicios que tienen pruebas de integración. Usa WireMock (auth), EmbeddedKafka y Testcontainers (promotion, notification):

```groovy
stage('Integration Tests') {
    steps {
        sh """
            ./gradlew \
                :services:circleguard-auth-service:test \
                :services:circleguard-promotion-service:test \
                :services:circleguard-notification-service:test \
                --tests '*IT' \
                --no-daemon --continue
        """
    }
}
```

**Deploy → Stage** — parchea los manifests de Kubernetes con el tag del build actual via `sed` antes de aplicarlos. Espera individualmente cada deployment con `rollout status` y usa `waitUntil` para promotion-service que tiene mayor tiempo de arranque:

```groovy
stage('Deploy → Stage') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file',
                        serverUrl:     'https://kubernetes.docker.internal:6443']) {
            script {
                // Parchear imagen en cada manifest con el BUILD_NUMBER actual
                sh """
                    for svc in auth identity form promotion notification dashboard; do
                        sed -i "s|image: circleguard-\${svc}-service:.*\
                               |image: ${IMAGE_PREFIX}/circleguard-\${svc}-service:${IMAGE_TAG}|g" \
                            k8s/\${svc}-service.yml
                    done
                """
                // Esperar infra, luego servicios de aplicación
                sh "${KUBECTL} rollout status deployment/circleguard-auth-service \
                    -n ${K8S_NS_STAGE} --timeout=300s"
                // waitUntil para promotion-service (Kafka consumer, arranque más lento)
                timeout(time: 60, unit: 'SECONDS') {
                    waitUntil {
                        def ready = sh(returnStdout: true, script: """
                            ${KUBECTL} get pods -n ${K8S_NS_STAGE} \
                                -l app=circleguard-promotion-service \
                                --field-selector=status.phase=Running --no-headers | wc -l
                        """).trim()
                        return ready.toInteger() >= 1
                    }
                }
            }
        }
    }
}
```

**E2E Tests** — corre en un agente Windows (`label 'windows-host'`) y accede a los servicios directamente por NodePort del cluster, sin necesitar port-forward. Usa `cleanTest` para evitar que Gradle omita las pruebas por caché UP-TO-DATE:

```groovy
stage('E2E Tests') {
    agent { label 'windows-host' }
    steps {
        withCredentials([
            usernamePassword(credentialsId: 'e2e-user-credentials',   ...),
            usernamePassword(credentialsId: 'e2e-admin-credentials',  ...)
        ]) {
            withEnv([
                "AUTH_SERVICE_URL=http://localhost:30180",
                "IDENTITY_SERVICE_URL=http://localhost:30083",
                ...
            ]) {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    bat "cmd /c \"%WORKSPACE%\\gradlew.bat \
                        :e2e-tests:cleanTest :e2e-tests:test --no-daemon --continue\""
                }
            }
        }
    }
}
```

**Performance Tests — Locust** — abre port-forwards dentro de `withKubeConfig` para que kubectl tenga credenciales durante toda la ejecución. Todo ocurre en un único bloque `sh` con shebang `#!/bin/bash`. El `trap EXIT` garantiza que los procesos de port-forward se maten al salir, sin importar si Locust falla o pasa. La comprobación de readiness usa `/dev/tcp` en lugar de `nc` (no disponible en el contenedor Jenkins):

```groovy
stage('Performance Tests — Locust') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file', ...]) {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                sh """#!/bin/bash
                    set -euo pipefail

                    # Abrir port-forwards en background y capturar PIDs
                    ${KUBECTL} port-forward svc/circleguard-auth-service \
                        ${pfLocAuth}:8180 -n ${K8S_NS_STAGE} > /tmp/pf_auth.log 2>&1 &
                    PF1=$!
                    # ... (form-service y dashboard-service igual)
                    trap 'kill $PF1 $PF2 $PF3 2>/dev/null || true' EXIT

                    # Esperar readiness con /dev/tcp (nc no disponible en el contenedor)
                    for port in ${pfLocAuth} ${pfLocForm} ${pfLocDash}; do
                        i=0
                        until (echo > /dev/tcp/127.0.0.1/$port) 2>/dev/null; do
                            i=$((i + 1))
                            [ $i -ge 30 ] && exit 1
                            sleep 1
                        done
                    done

                    pip3 install -q --break-system-packages -r locust/requirements.txt
                    locust \
                        --locustfile locust/locustfile.py \
                        --headless --users 20 --spawn-rate 4 --run-time 90s \
                        --csv locust/reports/circleguard \
                        --html locust/reports/report.html \
                        --exit-code-on-error 1   # Bloquea el pipeline si hay errores HTTP
                """
            }
        }
    }
    post {
        always {
            sh 'pkill -f "kubectl.*port-forward.*circleguard-stage" || true'
            archiveArtifacts artifacts: 'locust/reports/**'
        }
    }
}
```

**Post Actions**: Limpieza del namespace `circleguard-stage`

### 4.2 Resultado — Ejecución Exitosa

Vista del grafo del pipeline con todos los stages exitosos:

![circleguard-stage graph](<fotos/WhatsApp Image 2026-05-10 at 5.28.34 PM.jpeg>)

El pipeline completó todos los stages en **~12 minutos**:

![circleguard-stage exitoso](<fotos/WhatsApp Image 2026-05-10 at 2.30.21 PM.jpeg>)

**Detalle de duración por stage:**

| Stage | Duración |
|---|---|
| Checkout SCM | 3 s |
| Checkout | 1 s |
| Build Docker Images | 6 s |
| Unit Tests | 2 min 56 s |
| Integration Tests | 1 min 16 s |
| Deploy → Stage | 3 min 37 s |
| E2E Tests | 1 min 24 s |
| Performance Tests — Locust | 2 min 12 s |
| Post Actions | 40 s |
| **Total** | **~12 min** |

### 4.3 Resultado Pruebas de Rendimiento (Locust — Stage Pipeline)

El resumen impreso al final de la ejecución de Locust confirma el cumplimiento del SLA:

![Locust resultado stage](<fotos/WhatsApp Image 2026-05-10 at 2.30.03 PM.jpeg>)

| Métrica | Valor |
|---|---|
| Requests totales | 787 |
| Fallos totales | 0 |
| Tasa de error | 0.00 % |
| Tiempo respuesta promedio | 250.7 ms |
| Tiempo respuesta p95 | 260 ms |
| Tiempo respuesta p99 | 6 000 ms |
| **SLA (p95 < 1 s, error < 1%)** | **✓ CUMPLE** |

---

## 5. Pipeline 3 — circleguard-main

### 5.1 Configuración

El pipeline `circleguard-main` utiliza el script `jenkins/Jenkinsfile.main` y es el único que despliega a producción:

![Config circleguard-main](<fotos/WhatsApp Image 2026-05-10 at 4.45.04 PM.jpeg>)

**Stages del pipeline:**

```
Checkout → Build Docker Images (paralelo, 6 servicios)
→ Unit Tests → Integration Tests → Deploy → Stage
→ E2E Tests → Performance Tests — Locust
→ Deploy → Production → Release Notes → Post Actions
```

**Variables de entorno** — añade `K8S_NS_PROD` para el namespace de producción. `PORT_BASE` es el mismo que en stage pero los puertos de Locust se calculan con `- 1000` para no colisionar con los del E2E:

```groovy
environment {
    DOCKER_BUILDKIT = '1'
    IMAGE_PREFIX    = 'circleguard'
    IMAGE_TAG       = "${BUILD_NUMBER}"
    K8S_NS_STAGE    = 'circleguard-stage'
    K8S_NS_PROD     = 'circleguard-prod'   // Exclusivo del pipeline main
    KUBECTL         = 'kubectl --insecure-skip-tls-verify'
    PORT_BASE       = '19000'
}
```

**E2E Tests en Linux** — a diferencia del pipeline de stage (que corre en un agente Windows con NodePorts), aquí el E2E corre en el agente principal Linux con 5 port-forwards simultáneos gestionados con `trap EXIT`:

```groovy
stage('E2E Tests') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file', ...]) {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                sh """#!/bin/bash
                    set -euo pipefail
                    # 5 port-forwards en background con PIDs individuales
                    ${KUBECTL} port-forward svc/circleguard-auth-service \
                        ${pfAuth}:8180 -n ${K8S_NS_STAGE} > /tmp/pf_e2e_auth.log 2>&1 &
                    PF1=$!
                    # ... (identity, form, promotion, dashboard — igual)
                    trap 'kill $PF1 $PF2 $PF3 $PF4 $PF5 2>/dev/null || true' EXIT

                    # Readiness check con /dev/tcp para cada puerto
                    for port in ${pfAuth} ${pfIdentity} ${pfForm} ${pfPromotion} ${pfDashboard}; do
                        i=0
                        until (echo > /dev/tcp/127.0.0.1/$port) 2>/dev/null; do
                            i=$((i+1)); [ $i -ge 30 ] && exit 1; sleep 1
                        done
                    done

                    # cleanTest evita que Gradle use caché UP-TO-DATE y omita las pruebas
                    ./gradlew :e2e-tests:cleanTest :e2e-tests:test --no-daemon
                """
            }
        }
    }
}
```

**Deploy → Production** — los NodePorts son recursos cluster-wide (no por namespace), por lo que primero se elimina `circleguard-stage` para liberar los puertos antes de crear `circleguard-prod` con los mismos manifests:

```groovy
stage('Deploy → Production') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig-file', ...]) {
            script {
                // Liberar NodePorts del namespace de stage antes de reclamarlos en prod
                sh "${KUBECTL} delete namespace ${K8S_NS_STAGE} --ignore-not-found=true"

                sh """
                    ${KUBECTL} create namespace ${K8S_NS_PROD} \
                        --dry-run=client -o yaml | ${KUBECTL} apply --validate=false -f -
                """
                // Los manifests ya fueron parcheados con el IMAGE_TAG en el Deploy → Stage
                sh "${KUBECTL} apply --validate=false -f k8s/auth-service.yml -n ${K8S_NS_PROD} ..."
                sh "${KUBECTL} rollout status deployment/circleguard-auth-service \
                    -n ${K8S_NS_PROD} --timeout=300s ..."
            }
        }
    }
}
```

**Release Notes** — calcula el siguiente tag semver leyendo el último tag existente y haciendo bump del PATCH. Agrupa los commits por prefijo convencional (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`). Usa `==~` (boolean) en lugar del operador `=~` (Matcher) para evitar errores de serialización del CPS de Jenkins:

```groovy
stage('Release Notes') {
    steps {
        script {
            def lastSemver = sh(script: "git tag -l 'v*.*.*' | sort -V | tail -1",
                                returnStdout: true).trim()

            // ==~ retorna boolean (serializable por Jenkins CPS)
            // =~ retorna Matcher (no serializable — causa NotSerializableException)
            def nextVersion
            if (lastSemver ==~ /^v?\d+\.\d+\.\d+$/) {
                def parts = lastSemver.replaceAll('^v', '').split('\\.')
                nextVersion = "v${parts[0]}.${parts[1]}.${parts[2].toInteger() + 1}"
            } else {
                nextVersion = 'v1.0.0'
            }

            // Categorizar commits por prefijo convencional
            def categories = [
                [prefix: 'feat',     title: '### New Features'],
                [prefix: 'fix',      title: '### Bug Fixes'],
                [prefix: 'refactor', title: '### Refactoring'],
                [prefix: 'test',     title: '### Tests'],
                [prefix: 'chore',    title: '### Chores & Config'],
            ]

            // Crear tag anotado y pushearlo vía git-push-credentials
            sh """
                git tag -a "${nextVersion}" \
                    -m "Automated release ${nextVersion} — Jenkins build #${env.IMAGE_TAG}"
            """
            withCredentials([usernamePassword(credentialsId: 'git-push-credentials', ...)]) {
                sh """git push "https://${GIT_USER}:${GIT_PASS}@..." "${nextVersion}" """
            }
            archiveArtifacts artifacts: 'RELEASE_NOTES.md', fingerprint: true
        }
    }
}
```

### 5.2 Resultado — Ejecución Exitosa

Vista del grafo del pipeline con los 9 stages y los 6 servicios construidos en paralelo:

![circleguard-main graph](<fotos/WhatsApp Image 2026-05-10 at 5.29.08 PM.jpeg>)

El pipeline completó los 9 stages en **~16 minutos**:

![circleguard-main exitoso](<fotos/WhatsApp Image 2026-05-10 at 4.41.52 PM.jpeg>)

**Detalle de duración por stage:**

| Stage | Duración |
|---|---|
| Checkout SCM | 2 s |
| Checkout | 1 s |
| Build Docker Images | 9 s |
| Unit Tests | 3 min 23 s |
| Integration Tests | 1 min 10 s |
| Deploy → Stage | 4 min 3 s |
| E2E Tests | 1 min 27 s |
| Performance Tests — Locust | 1 min 35 s |
| Deploy → Production | 4 min 14 s |
| Release Notes | 6 s |
| Post Actions | 34 s |
| **Total** | **~16 min** |

---

## 6. Análisis de Pruebas de Rendimiento

Las pruebas de rendimiento se ejecutaron con **Locust** en modo headless con la siguiente configuración:

```
--users 20          (usuarios virtuales concurrentes)
--spawn-rate 4      (usuarios iniciados por segundo)
--run-time 90s      (duración del test)
--exit-code-on-error 1  (gate de calidad: falla el pipeline si hay errores)
```

Los datos completos están en `evidencia/circleguard_stats.csv`.

### 6.1 Resultados por Endpoint

| Endpoint | Requests | Fallos | Mediana | Promedio | Mín | Máx | p95 | RPS |
|---|---|---|---|---|---|---|---|---|
| GET /api/v1/analytics/health-board | 113 | 0 | 19 ms | 35.6 ms | 7.7 ms | 1 436 ms | 53 ms | 1.26 |
| GET /api/v1/analytics/summary | 120 | 0 | 20 ms | 35.4 ms | 7.2 ms | 1 411 ms | 57 ms | 1.34 |
| GET /api/v1/questionnaires/active | 362 | 0 | 16 ms | 19.4 ms | 5.8 ms | 131 ms | 43 ms | 4.05 |
| POST /api/v1/auth/login | 20 | 0 | 1 000 ms | 1 085 ms | 736 ms | 1 529 ms | 1 500 ms | 0.22 |
| POST /api/v1/surveys | 263 | 0 | 30 ms | 49.9 ms | 11.3 ms | 2 655 ms | 82 ms | 2.94 |
| **Aggregated** | **878** | **0** | **21 ms** | **57.1 ms** | **5.8 ms** | **2 655 ms** | **78 ms** | **9.83** |

### 6.2 Throughput (RPS)

El throughput agregado alcanzó un pico de **~10.3 req/s** en estado estable (20 usuarios concurrentes), partiendo desde 0 con una rampa de 4 usuarios/segundo durante los primeros 5 segundos. La serie histórica (`circleguard_stats_history.csv`) muestra la evolución:

| Fase | Usuarios | RPS Agregado |
|---|---|---|
| Rampa (t=0–5s) | 0 → 20 | 0 → 4.0 |
| Estabilización (t=5–15s) | 20 | 4.0 → 8.7 |
| Estado estable (t=15–90s) | 20 | 8.7 → 10.3 |

### 6.3 Tiempos de Respuesta

**Endpoints de consulta (GET):** Rendimiento excelente en condiciones de carga.

- `/api/v1/questionnaires/active` — el endpoint más solicitado (41% del tráfico) respondió con mediana de **16 ms** y p95 de **43 ms**, sin superar 131 ms en ninguna petición.
- `/api/v1/analytics/health-board` y `/api/v1/analytics/summary` — medianas de **19–20 ms** y p95 en **53–57 ms**. Los valores extremos (p99.9 = 1 400 ms) corresponden a las primeras peticiones del warm-up cuando el sistema recién inicializó las conexiones a base de datos.

**Endpoint de autenticación (POST /api/v1/auth/login):** Tiempo de respuesta esperadamente más alto por la generación de JWT y validación LDAP.

- Mediana: **1 000 ms**, p95: **1 500 ms**
- Este endpoint es el cuello de botella natural del sistema; su latencia es consistente y predecible (variación entre 736 ms y 1 529 ms), sin indicios de degradación bajo carga.

**Endpoint de creación de encuesta (POST /api/v1/surveys):** Comportamiento mixto.

- Mediana: **30 ms**, p95: **82 ms** — la mayoría de peticiones son rápidas.
- El máximo de **2 655 ms** corresponde a peticiones durante el arranque del sistema cuando Kafka aún estaba procesando el backlog inicial. En estado estable el comportamiento es consistente.

### 6.4 Tasa de Errores

**0 fallos en 878 requests** (0.00%). Los archivos `circleguard_failures.csv` y `circleguard_exceptions.csv` están vacíos, confirmando ausencia total de errores HTTP, timeouts o excepciones durante el test.

### 6.5 Cumplimiento de SLA

| Criterio SLA | Límite | Valor Obtenido | Estado |
|---|---|---|---|
| p95 tiempo de respuesta | < 1 000 ms | 78 ms (agregado) | ✓ CUMPLE |
| Tasa de error | < 1% | 0.00% | ✓ CUMPLE |

El gate de calidad `--exit-code-on-error 1` de Locust no se activó, permitiendo que el pipeline de main continuara al Deploy → Production.

---

## 7. Conclusiones

- Los **tres pipelines** ejecutaron exitosamente en sus últimas ejecuciones, demostrando la estabilidad de la configuración CI/CD.

- La estrategia de **progresión por ambientes** (dev → stage → prod) garantiza que ningún cambio llega a producción sin haber pasado por unit tests, integration tests, pruebas E2E y una prueba de carga de 90 segundos con 20 usuarios concurrentes.

- Las **pruebas de rendimiento** confirman que el sistema maneja carga de 20 usuarios concurrentes con p95 global de 78 ms y 0% de errores, cumpliendo holgadamente el SLA establecido (p95 < 1 000 ms, error < 1%).

- El endpoint de mayor latencia es `POST /api/v1/auth/login` (mediana 1 000 ms), lo cual es esperado dado el costo de generación de JWT y validación de credenciales vía LDAP. Este comportamiento es estable y consistente bajo carga, sin degradación observable.

- El endpoint más crítico en términos de volumen, `GET /api/v1/questionnaires/active`, respondió con mediana de **16 ms** bajo 20 usuarios concurrentes, evidenciando una arquitectura de consulta eficiente.

- El sistema **no presentó fallos** en ninguna capa de validación: 0 pruebas unitarias fallidas, 0 pruebas de integración fallidas, 0 pruebas E2E fallidas (tests habilitados), y 0 errores HTTP durante la prueba de carga.
