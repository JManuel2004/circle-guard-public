"""
CircleGuard — Pruebas de Rendimiento y Estrés con Locust
=========================================================

Escenario: campus member (usuario virtual) que realiza el flujo diario completo:
  1. Login          → auth-service   (8180)
  2. Formulario     → form-service   (8086)  [read]
  3. Encuesta       → form-service   (8086)  [write]
  4. Dashboard      → dashboard-service (8084) → llama internamente a promotion-service

═══════════════════════════════════════════════════════════════════════════════
DÓNDE APARECEN LAS TRES MÉTRICAS CLAVE EN LOCUST
═══════════════════════════════════════════════════════════════════════════════

  1. TIEMPO DE RESPUESTA (Response Time)
     ─────────────────────────────────────
     • Locust mide el tiempo total (ms) desde que el cliente envía la
       primera byte hasta que recibe el último byte de la respuesta HTTP.
     • Capturado AUTOMÁTICAMENTE para cada llamada self.client.*()
       cuando se usa catch_response=True (o sin él).
     • En la interfaz web (http://localhost:8089):
         → Statistics → columnas "50%ile", "75%ile", "95%ile", "99%ile" (ms)
         → Charts     → pestaña "Response Times (ms)"
     • En los archivos CSV generados con --csv=<prefix>:
         → <prefix>_stats.csv          columnas: "50%", "66%", "75%"... "99%", "100%"
         → <prefix>_stats_history.csv  serie temporal: percentiles cada ~2 s
     • NFR objetivo del proyecto: p95 < 1 000 ms para todos los endpoints.

  2. THROUGHPUT (Requests Per Second — RPS)
     ─────────────────────────────────────────
     • Locust cuenta cuántas requests completan por segundo, agregadas
       por el nombre asignado con el parámetro `name=`.
     • En la interfaz web:
         → Statistics → columna "Requests/s"
         → Charts     → pestaña "Total Requests per Second"
     • En los archivos CSV:
         → <prefix>_stats.csv          columna: "Requests/s"
         → <prefix>_stats_history.csv  columna: "Requests/s" (cada tick de ~2 s)
     • El throughput depende de: número de usuarios × (1 / (avg_response_time + wait_time))
       Aumentar `--users` sube el RPS hasta que algún servicio se satura.

  3. TASA DE ERRORES (Error Rate)
     ──────────────────────────────
     • Una request se cuenta como fallo cuando:
         a) El código HTTP es 4xx/5xx  (automático), O
         b) Se llama response.failure("motivo") explícitamente dentro de
            un bloque `with self.client.get(..., catch_response=True)`.
     • Tasa de error = (# Failures / # Requests) × 100
     • En la interfaz web:
         → Statistics → columnas "# Fails" y "Fail%"
         → Pestaña "Failures" → detalle por nombre y mensaje de error
     • En los archivos CSV:
         → <prefix>_stats.csv      columnas: "Failure Count"
         → <prefix>_failures.csv   columnas: "Method", "Name", "Error", "Occurrences"
     • SLA objetivo de referencia: tasa de error < 1 % bajo carga nominal.

═══════════════════════════════════════════════════════════════════════════════
"""

import os
import random
import uuid

from locust import HttpUser, task, between, events


# ─────────────────────────────────────────────────────────────────────────────
# CONFIGURACIÓN DE SERVICIOS
# Leer desde variables de entorno para reutilizar el script en CI/CD,
# docker-compose local y entornos de staging sin modificar el código.
#
#   export AUTH_SERVICE_URL=http://auth-service:8180      # en docker-compose
#   export FORM_SERVICE_URL=http://form-service:8086
#   export DASHBOARD_SERVICE_URL=http://dashboard-service:8084
# ─────────────────────────────────────────────────────────────────────────────
AUTH_URL      = os.getenv("AUTH_SERVICE_URL",      "http://localhost:8180")
FORM_URL      = os.getenv("FORM_SERVICE_URL",      "http://localhost:8086")
DASHBOARD_URL = os.getenv("DASHBOARD_SERVICE_URL", "http://localhost:8084")

# Credenciales de prueba — deben existir en las migraciones Flyway de auth-service
TEST_USER = os.getenv("E2E_USER", "testuser")
TEST_PASS = os.getenv("E2E_PASS", "password123")


# ─────────────────────────────────────────────────────────────────────────────
# USUARIO VIRTUAL
# ─────────────────────────────────────────────────────────────────────────────
class CircleGuardUser(HttpUser):
    """
    Representa un miembro del campus usando CircleGuard durante un día normal.

    host
    ────
    Locust usa `host` como URL base para llamadas con rutas relativas
    (e.g., self.client.post("/api/v1/auth/login")).
    Para los otros servicios se pasan URLs absolutas; Locust's HttpSession
    (basada en requests.Session) las respeta sin problema.

    wait_time
    ─────────
    `between(1, 3)` introduce una pausa aleatoria de 1–3 segundos DESPUÉS
    de cada @task.  Simula el "tiempo de reflexión" real del usuario.
    • Efecto en THROUGHPUT: más wait_time → menos RPS por usuario virtual.
    • Efecto en CARGA:      aumentar --users compensa el wait_time.
    • Esta pausa NO aparece como tiempo de respuesta en los CSV; sólo
      afecta cuántas veces por segundo se llama a cada tarea.
    """

    host      = AUTH_URL      # base URL para self.client.* con rutas relativas
    wait_time = between(1, 3) # pausa inter-tarea en segundos

    # Estado de sesión por usuario virtual
    token            : str | None = None
    anonymous_id     : str | None = None
    questionnaire_id : str | None = None   # cacheado después del primer fetch

    # ─────────────────────────────────────────────────────────────────────────
    # CICLO DE VIDA
    # ─────────────────────────────────────────────────────────────────────────

    def on_start(self):
        """
        Se ejecuta UNA SOLA VEZ por usuario virtual al arrancar, antes de
        cualquier @task.  Es el lugar correcto para el login porque:
          • El JWT es reutilizable durante toda la sesión del usuario.
          • El costo del login no debe inflarse artificialmente ejecutándolo
            como tarea repetida.

        IMPACTO EN MÉTRICAS:
          • El tiempo de respuesta del login aparece en la fila
            "POST /api/v1/auth/login" de <prefix>_stats.csv.
          • Si auth-service es lento, el arranque de usuarios será lento
            (visible como "ramp-up" prolongado en _stats_history.csv).
          • Un fallo aquí deja al usuario sin token; las tareas posteriores
            lo detectan y registran un evento de skip en lugar de fallar
            con NullPointerException.
        """
        with self.client.post(
            "/api/v1/auth/login",
            json={"username": TEST_USER, "password": TEST_PASS},
            name="POST /api/v1/auth/login",
            catch_response=True,         # necesario para llamar resp.failure()
        ) as resp:
            if resp.status_code == 200:
                data             = resp.json()
                self.token       = data.get("token")
                self.anonymous_id = data.get("anonymousId")
                resp.success()
            else:
                # Registra el fallo → aparece en <prefix>_failures.csv
                resp.failure(
                    f"Login failed [{resp.status_code}]: {resp.text[:200]}"
                )

    def on_stop(self):
        """Limpieza al finalizar la sesión del usuario virtual."""
        self.token        = None
        self.anonymous_id = None

    # ─────────────────────────────────────────────────────────────────────────
    # HELPERS INTERNOS
    # ─────────────────────────────────────────────────────────────────────────

    def _auth_headers(self) -> dict:
        """Cabecera Bearer para servicios que requieren JWT."""
        return {"Authorization": f"Bearer {self.token}"}

    def _guard(self) -> bool:
        """
        Devuelve True y dispara un evento sintético si el usuario no tiene JWT.
        Evita que tareas downstream fallen con excepciones no controladas cuando
        el login falló en on_start.

        El evento sintético es invisible en el mapa de percentiles pero sí cuenta
        en el total de "# Fails", lo que mantiene honesta la tasa de errores.
        """
        if not self.token:
            self.environment.events.request.fire(
                request_type="GUARD",
                name="[skipped — no JWT from login]",
                response_time=0,
                response_length=0,
                exception=RuntimeError("No JWT available; login failed on on_start"),
            )
            return True
        return False

    # ─────────────────────────────────────────────────────────────────────────
    # TAREAS  (@task)
    #
    # El decorador @task(weight) define la PROBABILIDAD RELATIVA con la que
    # Locust elige cada tarea.  Con pesos 3:2:1:1 el scheduler llama al
    # formulario 3 veces por cada 2 encuestas y por cada 1 llamada al dashboard.
    # Esto reproduce un perfil de carga realista: más lecturas que escrituras.
    #
    # CÓMO AFECTAN LOS PESOS A LAS MÉTRICAS:
    #   • Distribución de RPS: la fila "POST /api/v1/surveys" tendrá ~2/7 del
    #     RPS total; "GET /api/v1/questionnaires/active" tendrá ~3/7, etc.
    #   • Si una tarea pesada (e.g., POST survey) tiene latencia alta, su peso
    #     la hará aparecer más en los percentiles globales de _stats_history.csv.
    # ─────────────────────────────────────────────────────────────────────────

    @task(3)
    def get_active_questionnaire(self):
        """
        Paso 2 del flujo — leer el formulario activo.
        Peso 3: es la operación más frecuente (lectura pura, sin escritura en DB).

        MÉTRICAS CLAVE A OBSERVAR:
          Tiempo de respuesta
            → <prefix>_stats.csv fila "GET /api/v1/questionnaires/active"
            → Debe ser estable (< 200 ms) incluso bajo carga; un incremento
              indica contención en el pool de conexiones de form-service.
          Throughput
            → Columna "Requests/s" de la misma fila.
            → Al ser la tarea más frecuente, domina el RPS total de form-service.
          Errores
            → 404 se trata como éxito (sin cuestionario activo es estado válido).
            → Cualquier 5xx se marca fallo → <prefix>_failures.csv.
        """
        if self._guard():
            return

        with self.client.get(
            f"{FORM_URL}/api/v1/questionnaires/active",
            headers=self._auth_headers(),
            name="GET /api/v1/questionnaires/active",
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                body = resp.json()
                # Cachear el ID para enriquecer el payload de la encuesta
                self.questionnaire_id = body.get("id")
                resp.success()
            elif resp.status_code == 404:
                # Sin cuestionario activo — válido, no es un error de sistema
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code} on questionnaire fetch")

    @task(2)
    def submit_health_survey(self):
        """
        Paso 3 del flujo — enviar la encuesta de salud.
        Peso 2: operación de escritura menos frecuente que la lectura.

        MÉTRICAS CLAVE A OBSERVAR:
          Tiempo de respuesta
            → <prefix>_stats.csv fila "POST /api/v1/surveys"
            → Este endpoint involucra: validación → persistencia en PostgreSQL
              → publish en Kafka.  Un p95 > 500 ms bajo carga indica que
              el publish de Kafka o el commit de la transacción es el cuello
              de botella.  Visible en la curva de <prefix>_stats_history.csv.
          Throughput
            → Columna "Requests/s".  Al ser escritura, su RPS máximo estará
              limitado por el connection pool y el throughput de Kafka.
          Errores
            → Errores 503/504 suelen indicar agotamiento del pool de DB.
            → Comparar la columna "Failure Count" con los picos de p99.
        """
        if self._guard():
            return

        # Variar los síntomas para producir datos realistas y estresar
        # distintas ramas del SymptomMapper
        payload: dict = {
            "anonymousId": self.anonymous_id,
            "hasFever":    random.random() < 0.2,   # 20 % de usuarios con fiebre
            "hasCough":    random.random() < 0.15,
        }
        if self.questionnaire_id:
            payload["responses"] = {
                self.questionnaire_id: "YES" if payload["hasFever"] else "NO"
            }

        with self.client.post(
            f"{FORM_URL}/api/v1/surveys",
            json=payload,
            headers=self._auth_headers(),
            name="POST /api/v1/surveys",
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(
                    f"Survey rejected [{resp.status_code}]: {resp.text[:200]}"
                )

    @task(1)
    def get_dashboard_summary(self):
        """
        Paso 4a del flujo — resumen del campus desde dashboard-service.
        Peso 1: operación analítica, poco frecuente en el flujo diario.

        MÉTRICAS CLAVE A OBSERVAR:
          Tiempo de respuesta
            → <prefix>_stats.csv fila "GET /api/v1/analytics/summary"
            → Este endpoint cruza DOS fronteras de servicio (dashboard →
              promotion-service → Neo4j/Redis).  Es el indicador más
              sensible de degradación en cascada.
            → Si p95 aquí crece mientras los otros endpoints permanecen
              estables, el cuello de botella está en promotion-service.
          Throughput
            → Tendrá el RPS más bajo de las cuatro tareas (peso 1 / 7).
          Errores
            → Un 503 aquí probablemente refleja que promotion-service
              agotó su pool de conexiones Neo4j o sus threads de Redis.
        """
        if self._guard():
            return

        with self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/summary",
            headers=self._auth_headers(),
            name="GET /api/v1/analytics/summary",
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Dashboard summary [{resp.status_code}]: {resp.text[:200]}")

    @task(1)
    def get_dashboard_health_board(self):
        """
        Paso 4b del flujo — health board detallado (filtra con k-anonymity).
        Mismo peso que summary; proporciona un segundo punto de medición en
        dashboard-service para separar latencia propia de latencia de promotion.

        MÉTRICAS CLAVE A OBSERVAR:
          Si la diferencia de p95 entre esta fila y /summary es > 100 ms,
          el costo extra es el KAnonymityFilter o la serialización JSON del
          board completo — no promotion-service.
        """
        if self._guard():
            return

        with self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/health-board",
            headers=self._auth_headers(),
            name="GET /api/v1/analytics/health-board",
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Health board [{resp.status_code}]: {resp.text[:200]}")


# ─────────────────────────────────────────────────────────────────────────────
# HOOKS GLOBALES DEL RUNNER
# Se ejecutan una vez para todo el proceso Locust, no por usuario virtual.
# Son el lugar correcto para:
#   • Imprimir configuración al arranque.
#   • Conectar exportadores de métricas (Prometheus, InfluxDB).
#   • Imprimir resumen final antes de que el proceso termine.
# ─────────────────────────────────────────────────────────────────────────────

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """
    Dispara cuando Locust inicia el test (antes del primer spawn de usuarios).
    Imprime la configuración activa para que quede en el log del pipeline.
    """
    print("\n" + "═" * 68)
    print("  CircleGuard — Performance Test: INICIANDO")
    print(f"  AUTH      → {AUTH_URL}")
    print(f"  FORM      → {FORM_URL}")
    print(f"  DASHBOARD → {DASHBOARD_URL}")
    print("═" * 68)
    print("""
  GUÍA DE MÉTRICAS — DÓNDE LEER CADA VALOR
  ──────────────────────────────────────────────────────────────────
  Tiempo de respuesta │ *_stats.csv       │ 50%, 75%, 95%, 99%
  (ms por endpoint)   │ *_stats_history   │ evolución temporal
  ────────────────────┼───────────────────┼────────────────────────
  Throughput (RPS)    │ *_stats.csv       │ columna "Requests/s"
                      │ *_stats_history   │ columna "Requests/s"
  ────────────────────┼───────────────────┼────────────────────────
  Tasa de errores (%) │ *_stats.csv       │ "Failure Count" / Total
                      │ *_failures.csv    │ detalle por endpoint
  ──────────────────────────────────────────────────────────────────
  Reporte HTML interactivo: ver archivo report.html
    """)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """
    Dispara cuando el runner termina (después de --run-time o Ctrl+C).
    Imprime un resumen compacto de las tres métricas clave.
    Los valores exactos estarán en los archivos CSV/HTML generados.

    NOTA SOBRE LAS MÉTRICAS AQUÍ VS EN EL CSV:
      Los valores impresos aquí son los acumulados de TODA la prueba.
      Los CSV contienen tanto el agregado (*_stats.csv) como la evolución
      temporal (*_stats_history.csv), que permite detectar degradación
      progresiva (típico en pruebas de estrés prolongadas).
    """
    total = environment.stats.total
    fails = total.num_failures
    reqs  = total.num_requests
    rate  = (fails / reqs * 100) if reqs > 0 else 0.0
    p95   = total.get_response_time_percentile(0.95)
    p99   = total.get_response_time_percentile(0.99)

    print("\n" + "═" * 68)
    print("  CircleGuard — Performance Test: FINALIZADO")
    print(f"  Requests totales  : {reqs:,}")
    print(f"  Fallos totales    : {fails:,}")
    print(f"  Tasa de error     : {rate:.2f} %")
    print(f"  Tiempo resp. avg  : {total.avg_response_time:.1f} ms")
    print(f"  Tiempo resp. p95  : {p95 or 'N/A'} ms")
    print(f"  Tiempo resp. p99  : {p99 or 'N/A'} ms")
    sla = "✓ CUMPLE" if (p95 or 9999) < 1000 and rate < 1.0 else "✗ NO CUMPLE"
    print(f"  SLA (p95<1s, err<1%): {sla}")
    print("═" * 68 + "\n")
