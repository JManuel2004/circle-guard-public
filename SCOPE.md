# Alcance del Taller — CircleGuard

El enunciado del taller exige un mínimo de **6 microservicios que se comuniquen entre sí**.
Este proyecto implementa exactamente 6 servicios que forman un flujo funcional completo de extremo a extremo.

---

## Microservicios incluidos (6)

| # | Microservicio | Directorio | Rol en el flujo |
|---|---|---|---|
| 1 | **auth-service** | `services/circleguard-auth-service/` | Autenticación LDAP, emisión de JWT y anonymousId. Punto de entrada de todos los flujos. |
| 2 | **identity-service** | `services/circleguard-identity-service/` | Vault de identidades reales encriptadas. Recibe llamadas de auth-service vía HTTP. |
| 3 | **form-service** | `services/circleguard-form-service/` | Gestión de cuestionarios y encuestas de salud. Publica eventos en Kafka (`survey.submitted`). |
| 4 | **promotion-service** | `services/circleguard-promotion-service/` | Motor de estados de salud y gestión de círculos. Consume `survey.submitted` de Kafka, actualiza Neo4j y Redis, y publica eventos de certificado. |
| 5 | **notification-service** | `services/circleguard-notification-service/` | Dispatcher de alertas multicanal (email, SMS, push). Consume eventos de Kafka producidos por promotion-service. |
| 6 | **dashboard-service** | `services/circleguard-dashboard-service/` | Analítica agregada con k-anonymity. Consulta promotion-service para datos en tiempo real. |

**Flujo principal:** `auth-service → identity-service → form-service → promotion-service → notification-service → dashboard-service`

Cada enlace está cubierto por pruebas de integración o E2E que validan la comunicación entre servicios.

---

## Microservicios excluidos (2)

| Microservicio | Directorio | Razón de exclusión |
|---|---|---|
| **gateway-service** | `services/circleguard-gateway-service/` | No aparece en el pipeline de build ni en los tests de CI. Nunca fue integrado al flujo de comunicación entre los 6 servicios principales. Su función de validación QR-Redis es redundante con el JWT que ya emite auth-service. |
| **file-service** | `services/circleguard-file-service/` | Servicio huérfano: tiene Dockerfile y manifiesto K8s pero ningún otro servicio lo llama ni lo referencia. No tiene pruebas de integración ni aparece en ningún flujo E2E. No está conectado al flujo principal del sistema. |

Los manifiestos K8s de estos servicios se conservan en el repositorio como referencia bajo los nombres
`k8s/gateway-service.yml.excluded` y `k8s/file-service.yml.excluded`. No son procesados por `kubectl apply`.

---

*El enunciado del taller establece un mínimo de 6 microservicios que se comuniquen entre sí. Este proyecto cumple exactamente ese requisito con los 6 servicios listados arriba.*
