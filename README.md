# TurnoFácil API

Microservicio REST para gestión de reservas de atención — **Spring Boot 3.3 + Java 21**

## Integrantes
- Maximiliano Rodriguez Gamboa
- Benjamín Dattoli Peña
- Vicente Fabar

## Stack
`Java 21` `Spring Boot 3.3` `Spring Data JPA` `H2` `Validation` `OpenAPI/Swagger` `Actuator` `Lombok` `Maven` `JUnit 5`

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/health/liveness` | Kubernetes liveness probe |
| `GET` | `/health/readiness` | Kubernetes readiness probe |
| `POST` | `/reservas` | Crear reserva (valida fecha futura) |
| `GET` | `/reservas` | Listar reservas (`?estado=`) |
| `GET` | `/reservas/{id}` | Obtener por ID |

---

## GitFlow — Estrategia de Ramificación

**Justificación:** Entregas controladas, hotfixes independientes, trazabilidad completa, estándar empresarial.

```
main ──────●────────────────●──── (producción + hotfix)
            \              /
develop ─────●───●───●──────●──── (integración + 2 features)
              \   \   \
feature/validacion  ●───●─────── (Maximiliano: @Future en fechaHora)
                     \   \
feature/endpoint-salud  ●───●─── (Benjamín: liveness/readiness)
                           \
hotfix/fix-null-pointer ──────●── (Vicente: fix NPE espacios en blanco)
```

| Rama | Propósito | Merge a |
|------|-----------|---------|
| `main` | Producción | — |
| `develop` | Integración continua | `main` |
| `feature/*` | Nueva funcionalidad | `develop` |
| `hotfix/*` | Bug urgente producción | `main` + `develop` |

---

## Convenciones

**Commits (Conventional Commits):**
```
feat: agrega validación @Future en fechaHora para reservas
feat: mejora endpoint /health con liveness, readiness y metadatos
fix: normaliza estado en listarReservas para evitar NPE
ci: simplifica workflow para estudiante
```

**Merge:** `--no-ff` obligatorio · PR requerido · CI verde · Code review (1 aprobación) · Eliminar rama tras merge

---

## ⚙️ CI/CD — GitHub Actions (`.github/workflows/ci.yml`)

**Triggers:** `push` a `develop` · `pull_request` a `main`

**Jobs:**
| Job | Qué hace |
|-----|----------|
| `build-and-test` | Compila, testea (12 tests), empaqueta JAR |
| `code-quality` | Spotless (formato) — `continue-on-error: true` |

**Fix crítico:** `chmod +x mvnw` en runners Linux

---

## Tests (12 totales)

```bash
./mvnw clean test
```

| Rama | Tests agregados |
|------|-----------------|
| `feature/validacion-reserva` | Fecha pasada → 400 |
| `feature/endpoint-salud` | `/liveness`, `/readiness` |
| `hotfix/fix-null-pointer` | Estado `null`/`""`/`"  "` |

---

## Ejecución Local

```bash
git clone https://github.com/maxrodriguezg/turnofacil-api.git
cd turnofacil-api
./mvnw clean test
./mvnw spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/health
- H2 Console: http://localhost:8080/h2-console

---

## Estructura MVC

```
src/main/java/com/turnofacil/api/
├── controller/     # HealthController, ReservaController
├── dto/            # ReservaCreateRequest, ReservaResponse, ErrorResponse
├── service/        # ReservaService, ReservaServiceImpl
├── repository/     # ReservaRepository (JPA)
├── model/          # Reserva (Entity)
└── exception/      # GlobalExceptionHandler, ReservaNotFoundException
```

---

## PRs Simulados 

| PR | Autor | Rama | Tipo | Cambios |
|----|-------|------|------|---------|
| #1 | Maximiliano | `feature/validacion-reserva` | Feature | `@Future` en fechaHora + test |
| #2 | Benjamín | `feature/endpoint-salud` | Feature | liveness/readiness + 2 tests |
| #3 | Vicente | `hotfix/fix-null-pointer` | Hotfix | `trim()` + `isBlank()` en estado + test |

---

## Seguridad
- Bean Validation (`@NotNull`, `@Size`, `@Future`)
- `@RestControllerAdvice` centralizado
- Sin credenciales hardcodeadas
- DTOs separados de entidades

---

---

## Conclusiones Individuales

### Maximiliano Rodriguez Gamboa
> *En este trabajo aprendí como funciona la integración continua trabajando con GitHub actions. Puede entender como y porque debemos automatizar procesos como pruebas y despliegues de una app que estemos desarrollando, es mucho mas eficiente que estar realizando estas pruebas manualmente cada vez que hacemos un cambio, aprendi que es muy importante usar bien los repositorios, y comprendi mucho mejor el manejo de ramas ya que era algo que me costaba mucho, también aprendí a utilizar los Pull Request para proponer y revisar cambios antes de integrarlos a la rama principal, los workflows también fueron importantes para mantener el proyecto mas organizado. Siento que esta experiencia me ayudo a entender mejor como se trabajo con ci/cd en proyextos reales y como puede facilitar el desarrollo y mantenimiento de una app. Con respecto a mi colaboración en el trabajo, me encargue de realizar la presentación, crear el repositorio y sus ramas, e implementar el cambio feat - agrega validación @future en fechaHora para reservas.*

### Benjamín Dattoli Peña
> *En este trabajo fortalecí mi comprensión de la integración continua con GitHub Actions y la importancia de automatizar pruebas para mantener un código estable. Además, desarrollé un dominio profundo de Git al liderar la recuperación técnica del repositorio tras una pérdida crítica de historiales, resolviendo desincronizaciones y conflictos de fusión complejos para unificar el proyecto. En cuanto a mi colaboración, solucioné errores clave en el código como problemas de importación y un NullPointerException en el servicio de reservas, asegurando que las ramas se integraran correctamente a través de Pull Requests y garantizando el flujo continuo del desarrollo.*

### Vicente Fabar
> *Durante este trabajo pude aprender mucho más sobre la integración continua y el uso de GitHub Actions. Entendí mejor la importancia de automatizar tareas como las pruebas y los despliegues, ya que esto permite ahorrar tiempo y evitar tener que realizar estos procesos manualmente cada vez que se hace una modificación en el proyecto. También pude mejorar mi manejo de los repositorios y, especialmente, de las ramas, que era un tema que anteriormente me costaba bastante. Además, aprendí a utilizar los Pull Request para revisar y organizar los cambios antes de incorporarlos a la rama principal, junto con los workflows para mantener un flujo de trabajo más ordenado.*

---

## Declaración IA
Herramientas: GitHub Copilot / Claude. Uso: documentación, pipeline, arquitectura. Todo validado con `./mvnw test`.

---

**DOY0101 Ingeniería DevOps — Evaluación Parcial 1**  
Repo: `https://github.com/maxrodriguezg/turnofacil-api`
