# TurnoFácil API

[![CI](https://github.com/maxrodriguezg/turnofacil-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/maxrodriguezg/turnofacil-api/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=maxrodriguezg_turnofacil-api&metric=alert_status)](https://sonarcloud.io/project/overview?id=maxrodriguezg_turnofacil-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=maxrodriguezg_turnofacil-api&metric=coverage)](https://sonarcloud.io/project/overview?id=maxrodriguezg_turnofacil-api)

Microservicio REST para la gestión de reservas de atención, construido con Java 21 y Spring Boot 4.0.9. Forma parte de TurnoFácil junto con la web React y se despliega en AWS ECS Fargate con Service Connect, observabilidad en CloudWatch y pipeline CI/CD en GitHub Actions.

---

## EP3 — Observabilidad y despliegue en AWS ECS

> Esta sección describe el estado actual de la EP3. Las secciones posteriores sobre Docker Compose corresponden al entorno local y a etapas anteriores.

TurnoFácil está compuesto por:

- **API REST** en Spring Boot 4.0.9.
- **Web** en React + Nginx.
- **PostgreSQL 16**.
- Despliegue en **AWS ECS Fargate** con **Service Connect**.
- Observabilidad con **CloudWatch**, **SNS** y trazas con **ADOT/OpenTelemetry**.

Repositorios:

- API: https://github.com/maxrodriguezg/turnofacil-api
- Web: https://github.com/vf-organization/turnofacil-web

### Arquitectura

```text
Internet
   │ 8080
   ▼
[Web ECS] ──proxy /api──► [API ECS ×2] ──► [PostgreSQL ECS]
   │                         │
   └──── Service Connect ─────┘
                             │
                  CloudWatch Metrics + Logs
                             │
                  Dashboard + Alarma 5XX
                             │
                            SNS → correo
```

- Solo la **web** es pública.
- La API y la base de datos solo aceptan tráfico interno.
- La API tiene **2 réplicas**.
- La web resuelve la API por `turnofacil-api:8080`.
- La API resuelve la base por `db:5432`.

### Stack de la EP3

| Componente | Tecnología |
|---|---|
| Lenguaje API | Java 21 |
| Framework API | Spring Boot 4.0.9 |
| Persistencia | Spring Data JPA + PostgreSQL 16 |
| Frontend | React 19 + Vite 8 + Nginx |
| Orquestación | ECS Fargate + Service Connect |
| Registro de imágenes | Amazon ECR |
| Métricas y logs | CloudWatch + Container Insights |
| Alertas | CloudWatch Alarms + SNS |
| Trazas | ADOT/OpenTelemetry → X-Ray |
| CI/CD | GitHub Actions |
| Calidad | SonarCloud, JaCoCo, Snyk |
| Contenedores | Docker multi-stage |

### Despliegue en ECS

| Workflow | Función |
|---|---|
| `Bootstrap ECS` | Crea clúster, namespace, logs, red, SGs, ECR y PostgreSQL |
| `Deploy API - ECS Fargate` | Build → ECR → task definition → servicio API |
| `Deploy Web - ECS Fargate` | Build → ECR → task definition → servicio web |

Orden obligatorio:

1. **Bootstrap ECS**
2. **Deploy API**
3. **Deploy Web**

Características:

- Las imágenes se publican en ECR.
- La API usa `appProtocol: http` para las métricas de Service Connect.
- La web se despliega después porque Nginx resuelve `turnofacil-api` al arrancar.
- El deploy de `main` se ejecuta al hacer merge.
- La IP pública de la web cambia cuando ECS reemplaza la tarea.

Secrets necesarios:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`
- `POSTGRES_PASSWORD` (solo API)

> Las credenciales de AWS Academy caducan al terminar la sesión. Hay que actualizarlas antes de cada despliegue.

### Observabilidad

Dashboard:

- **Nombre:** `Dashboard-Mesh-turnofacil`
- **URL:** https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=Dashboard-Mesh-turnofacil

Widgets:

1. CPU por servicio.
2. Memoria por servicio.
3. Requests y latencia del mesh.
4. Códigos HTTP 2XX/5XX.
5. Logs de errores y warnings.
6. Tiempo de despliegue.
7. Cobertura de pruebas.

Métricas custom en `TurnoFacil/CI`:

| Métrica | Servicio | Valor registrado |
|---|---|---:|
| `CoveragePercent` | API | 97.4% |
| `CoveragePercent` | Web | 97.53% |
| `DeploymentDurationSeconds` | API | 382 s |
| `DeploymentDurationSeconds` | Web | 243 s |

Alarma:

- **Nombre:** `Mesh-5XX-turnofacil`
- Métrica: `HTTPCode_Target_5XX_Count`
- Dimensión: `TargetDiscoveryName=turnofacil-api`
- Umbral: 5 errores en 60 s
- Acción: SNS `turnofacil-mesh-alertas` → correo

Historial verificado:

```text
INSUFFICIENT_DATA → OK → ALARM → OK
```

Prueba de fallo realizada:

1. Se bajó PostgreSQL a 0.
2. Se generaron peticiones.
3. La API devolvió 5XX.
4. La alarma pasó a `ALARM` y envió correo.
5. Se restauró PostgreSQL.
6. Se forzó redeploy de la API.
7. La app volvió a `200/201`.
8. La alarma volvió a `OK`.

### Calidad y seguridad

- **SonarCloud:** Quality Gate bloqueante en API y web.
- **Snyk:** vulnerabilidades altas bloquean el pipeline.
- **JaCoCo:** mínimo 60% de cobertura en API.
- **Branch protection:** `main` y `develop` con PR obligatorio, checks requeridos, sin force-push y sin borrado.
- **Acciones de GitHub:** fijadas a SHA completo.
- **Web:** `npm ci --ignore-scripts`.
- **Red:** API y PostgreSQL no son accesibles desde internet; solo la web expone 8080.

### Trazabilidad con X-Ray

La API incluye:

- agente ADOT;
- sidecar `otel-collector`;
- exportador `awsxray`.

Estado actual: **configurado, pero bloqueado por permisos de AWS Academy**.

```text
AccessDeniedException:
LabRole is not authorized to perform: xray:PutTraceSegments
```

Solución requerida por un administrador:

```text
Adjuntar AWSXRayDaemonWriteAccess a LabRole
```

No requiere cambios de código.

### Mapeo con la rúbrica

| IE | Evidencia |
|---|---|
| IE1 | Dashboard, logs, métricas, alarma 5XX y correo |
| IE2 | ECS Fargate + Service Connect + deploy automático + ADOT configurado |
| IE3 | Cobertura y tiempo de despliegue en el dashboard |
| IE4 | Este README, slides y video |
| IE5 | SonarCloud, Snyk y branch protection |
| IE6 | PR #77 rojo y bloqueado por checks |

### Evidencias

- PR de fallo controlado: https://github.com/maxrodriguezg/turnofacil-api/pull/77
- Bootstrap: https://github.com/maxrodriguezg/turnofacil-api/actions/runs/44837804523
- Deploy API: https://github.com/maxrodriguezg/turnofacil-api/actions/runs/44953972742
- Deploy Web: https://github.com/vf-organization/turnofacil-web/actions/runs/44953978194
- CI API en main: https://github.com/maxrodriguezg/turnofacil-api/actions/runs/44953972796
- CI Web en main: https://github.com/vf-organization/turnofacil-web/actions/runs/44955054897

### Limitaciones

- AWS Academy no permite modificar IAM: X-Ray queda bloqueado hasta que un administrador adjunte la política.
- Las credenciales de Academy expiran.
- PostgreSQL en Fargate usa almacenamiento efímero: al reemplazar la tarea se pierden los datos y la API debe redeployarse para crear las tablas.
- La IP pública de la web cambia al reemplazar la tarea.

### Guion sugerido para el video

1. Abrir la web y mostrar reservas.
2. Crear una reserva y mostrar `201`.
3. Mostrar los servicios ECS: API 2/2, web 1/1, BD 1/1.
4. Abrir el dashboard y mostrar las cuatro familias de métricas.
5. Mostrar `CoveragePercent` y `DeploymentDurationSeconds`.
6. Mostrar la alarma y el correo de alerta/recuperación.
7. Mostrar el PR #77 rojo y bloqueado.
8. Mostrar branch protection y SonarCloud.
9. Mencionar la limitación de X-Ray por IAM.

---

## Integrantes

- Maximiliano Rodriguez Gamboa
- Benjamín Dattoli Peña
- Vicente Fabar

## Descripción

La API permite registrar reservas de atención, consultarlas por estado y obtener una reserva por su identificador. Expone endpoints de salud para monitoreo y documentación interactiva con OpenAPI/Swagger.

El proyecto aplica una estrategia GitFlow con ramas `main`, `develop`, `feature/*` y `hotfix/*`, integración mediante Pull Requests y validación automática en cada cambio.

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.9 |
| Persistencia | Spring Data JPA |
| Base de datos | H2 en memoria (desarrollo) / PostgreSQL 16 (producción) |
| Validación | Jakarta Bean Validation |
| Documentación | springdoc-openapi 3.0.3 / Swagger UI |
| Monitoreo | Spring Boot Actuator |
| Utilidades | Lombok |
| Build | Maven Wrapper 3.9.6 |
| Pruebas | JUnit 5, Mockito, MockMvc |
| Cobertura | JaCoCo 0.8.12 |
| Calidad | SonarCloud |
| Seguridad de dependencias | Snyk + Dependabot |
| Contenedores | Docker + ECS Fargate (local: Docker Compose) |
| Orquestación | ECS Fargate + Service Connect |
| Observabilidad | CloudWatch + SNS + ADOT |
| CI/CD | GitHub Actions + ECR + GHCR |

## Arquitectura

```text
Cliente HTTP
     |
     v
Spring Boot API (8080)
  - Controller
  - Service
  - Repository (JPA)
     |
     +--> H2 en memoria        (perfil dev, por defecto)
     |
     +--> PostgreSQL 16        (perfil prod, Docker Compose)

GitHub Actions
  checkout -> tests + JaCoCo -> SonarCloud -> Snyk
           -> imagen Docker (GHCR)
           -> deploy con Docker Compose (solo main)
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/health` | Health check de la aplicación |
| `GET` | `/health/liveness` | Liveness probe |
| `GET` | `/health/readiness` | Readiness probe |
| `POST` | `/reservas` | Crear una reserva (valida fecha futura) |
| `GET` | `/reservas` | Listar reservas (`?estado=pendiente`) |
| `GET` | `/reservas/{id}` | Obtener una reserva por ID |
| `GET` | `/actuator/health` | Health de Actuator |
| `GET` | `/actuator/info` | Información de Actuator |
| `GET` | `/swagger-ui.html` | Documentación interactiva |
| `GET` | `/api-docs` | Especificación OpenAPI |

### Ejemplo de creación

```bash
curl -X POST http://localhost:8080/reservas \
  -H 'Content-Type: application/json' \
  -d '{
    "cliente": "Juan Pérez",
    "servicio": "Corte de cabello",
    "fechaHora": "2026-11-01T10:30:00"
  }'
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "cliente": "Juan Pérez",
  "servicio": "Corte de cabello",
  "fechaHora": "2026-11-01T10:30:00",
  "estado": "pendiente"
}
```

## Modelo y validaciones

La entidad `Reserva` contiene:

| Campo | Regla |
|---|---|
| `id` | Generado automáticamente |
| `cliente` | Obligatorio, entre 2 y 80 caracteres |
| `servicio` | Obligatorio, entre 2 y 80 caracteres |
| `fechaHora` | Obligatoria y futura (`@Future`) |
| `estado` | Por defecto `pendiente` |

Los DTO de entrada y salida están separados de la entidad para no exponer el modelo de persistencia.

## Manejo de errores

`GlobalExceptionHandler` centraliza las respuestas de error con un formato uniforme (`ErrorResponse`):

| Situación | Código |
|---|---|
| Recurso no encontrado | `404 Not Found` |
| Datos inválidos o regla de negocio | `400 Bad Request` |
| JSON mal formado | `400 Bad Request` |
| Tipo de parámetro incorrecto | `400 Bad Request` |
| `Content-Type` no soportado | `415 Unsupported Media Type` |
| Método HTTP no permitido | `405 Method Not Allowed` |
| Error interno | `500 Internal Server Error` |

## Ejecución local

### Requisitos

- Java 21
- Docker y Docker Compose 

### Perfil dev con H2

Por defecto la aplicación usa H2 en memoria y crea el esquema al iniciar:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### Perfil prod con Docker Compose

El compose levanta PostgreSQL y la API con el perfil `prod`:

```bash
cp .env.example .env
# Editar .env y definir POSTGRES_PASSWORD
docker compose up --build -d
docker compose logs -f api
```

Para detener y limpiar los datos:

```bash
docker compose down -v
```

### URLs locales

| Servicio | URL |
|---|---|
| API | `http://localhost:8080` |
| Health | `http://localhost:8080/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| H2 Console (solo dev) | `http://localhost:8080/h2-console` |

## Docker y orquestación (entorno local / legado)

> **EP3:** el despliegue real es ECS Fargate con Service Connect. Esta sección describe el uso local con Docker Compose.

### Dockerfile

El `Dockerfile` usa una construcción multi-stage:

1. **Build:** `maven:3.9-eclipse-temurin-21` compila y empaqueta el JAR.
2. **Runtime:** `eclipse-temurin:21-jre-alpine` ejecuta la aplicación con una imagen liviana.
3. La aplicación corre con un usuario no root (`app`).
4. `JAVA_OPTS` limita la memoria al 75% del contenedor.
5. El healthcheck consulta `/health/liveness`.

### Docker Compose

| Servicio | Imagen | Función |
|---|---|---|
| `db` | `postgres:16-alpine` | Base de datos PostgreSQL |
| `api` | `ghcr.io/maxrodriguezg/turnofacil-api` o build local | Microservicio Spring Boot |

Características de la orquestación:

- `depends_on` con `condition: service_healthy` para esperar a PostgreSQL.
- Healthchecks en ambos servicios (`pg_isready` y `/health/readiness`).
- Volumen nombrado `pgdata` para persistir la base de datos.
- Límites de recursos para la API (`1.0` CPU y `512M` de memoria).
- Política `restart: unless-stopped`.
- Perfil `prod` con variables de entorno para la conexión a PostgreSQL.

## Pipeline CI/CD

Archivo: `.github/workflows/ci.yml`.

> **EP3:** además de este pipeline, existen `bootstrap-ecs.yml` y `deploy-ecs.yml` para el despliegue en ECS. Ver la sección EP3 al inicio.

### Disparadores

- `push` a `main` y `develop`.
- `pull_request` hacia `main` y `develop`.

### Flujo

```text
push / PR
   |
   v
test: tests + JaCoCo + SonarCloud
   |
   +--> security: Snyk dependencias
   |
   v
image: build Docker + Snyk imagen + push a GHCR
   |
   v
deploy: Docker Compose + smoke test (solo push a main)
   |
   v
notify: alerta si falla (solo push a main/develop)
```

### Jobs

| Job | Qué hace | Cuándo corre |
|---|---|---|
| `test` | Ejecuta `./mvnw clean verify`, genera JaCoCo, publica el artefacto y analiza con SonarCloud | Push y PR |
| `security` | Escanea dependencias con Snyk (`--severity-threshold=high`) | Push y PR |
| `image` | Construye la imagen, la escanea con Snyk y la publica en GHCR | Push y PR (push solo en `push`) |
| `deploy` | Levanta PostgreSQL + API con Compose, espera el health y ejecuta un smoke test | Solo push a `main` |
| `notify` | Crea o actualiza un issue con el fallo del pipeline | Push a `main`/`develop` con fallo |

### SonarCloud

- Proyecto: `maxrodriguezg_turnofacil-api`
- Organización: `maxrodriguezg`
- Host: `https://sonarcloud.io`
- Cobertura importada desde `target/site/jacoco/jacoco.xml`
- Configuración en `pom.xml` y en el step `Analisis de calidad (SonarCloud)`
- Requiere `fetch-depth: 0` en el checkout para el análisis correcto

El Quality Gate actual está en OK con una cobertura cercana al 97.5%.

### Cobertura con JaCoCo

- Se ejecuta en la fase `verify`.
- Genera reporte HTML/XML/CSV en `target/site/jacoco/`.
- El pipeline exige un mínimo de **60%** de cobertura de líneas.
- El reporte se publica como artefacto `reporte-jacoco` por 7 días.

### Snyk

- Escanea dependencias Maven con `snyk/actions/maven-3-jdk-21`.
- Escanea la imagen Docker construida.
- Usa la política `.snyk` para excepciones documentadas.
- Si encuentra vulnerabilidades de severidad alta, el pipeline **falla** (no usa `continue-on-error`).

### Dependabot

Configuración en `.github/dependabot.yml`. Dependabot lee este archivo desde la rama por defecto (`main`), pero abre los PRs contra `develop`.

| Ecosistema | Frecuencia | Límite de PRs | Agrupación |
|---|---|---|---|
| Maven | Semanal (lunes) | 5 | Minor y patch |
| GitHub Actions | Semanal (lunes) | 3 | Todas las actions |
| Docker | Semanal (lunes) | 2 | Sin agrupación |
| Docker Compose | Semanal (lunes) | 2 | Sin agrupación |

Se ignoran automáticamente los cambios mayores de Spring Boot y Postgres, y todas las actualizaciones de las imágenes `maven` y `eclipse-temurin` (se actualizan manualmente al cambiar la versión de Java).

## Trazabilidad

La trazabilidad se garantiza en cada etapa:

1. **Commit:** cada ejecución queda asociada al SHA de Git.
2. **Pruebas y cobertura:** el resultado queda en los logs y en el artefacto `reporte-jacoco`.
3. **Calidad:** SonarCloud registra el análisis y el Quality Gate del commit.
4. **Seguridad:** Snyk reporta dependencias e imagen escaneadas.
5. **Imagen:** se publica en GHCR como `ghcr.io/maxrodriguezg/turnofacil-api:<sha>`.
6. **Despliegue:** el job `deploy` usa exactamente esa imagen, ejecuta un smoke test y escribe un resumen con commit, imagen y actor en `GITHUB_STEP_SUMMARY`.
7. **Post-despliegue:** los logs de Compose quedan en la ejecución y el entorno se limpia con `docker compose down -v`.

## Estrategia de ramas

| Rama | Propósito | Integración |
|---|---|---|
| `main` | Producción y despliegue | Merge desde `develop` |
| `develop` | Integración continua | Features y fixes |
| `feature/*` | Nuevas funcionalidades | PR a `develop` |
| `hotfix/*` | Correcciones urgentes | PR a `main` y `develop` |

Convenciones:

- Conventional Commits (`feat`, `fix`, `ci`, `chore`, `docs`).
- Pull Request obligatorio y CI en verde.
- Merge con `--no-ff` para conservar el historial.
- Dependabot apunta a `develop`.

## Pruebas

```bash
./mvnw clean verify
```

| Clase | Tipo | Cantidad |
|---|---|---|
| `ReservaControllerIntegrationTest` | Integración con MockMvc y contexto Spring | 12 |
| `ReservaServiceImplTest` | Unitaria con Mockito | 6 |
| `GlobalExceptionHandlerTest` | Unitaria de manejo de errores | 5 |
| **Total** | | **23** |

Las pruebas cubren creación, listado, búsqueda, validaciones, errores de formato, códigos 404/405/415 y reglas de negocio.

## Secrets y variables

### GitHub Actions

| Nombre | Uso |
|---|---|
| `SONAR_TOKEN` | Análisis en SonarCloud |
| `SNYK_TOKEN` | Escaneo de dependencias e imagen |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL en el despliegue |
| `GITHUB_TOKEN` | Publicación de imagen en GHCR (automático) |

### Dependabot

| Nombre | Uso |
|---|---|
| `SONAR_TOKEN` | SonarCloud en PRs de Dependabot |
| `SNYK_TOKEN` | Snyk en PRs de Dependabot |

### Variables locales

| Variable | Descripción |
|---|---|
| `POSTGRES_DB` | Nombre de la base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring (`prod` para Compose) |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos |

Las credenciales no se versionan: `.env` está en `.gitignore` y los valores reales viven en los secrets de GitHub.

## Seguridad

- Bean Validation en los DTO de entrada.
- Manejo centralizado de excepciones sin filtrar detalles internos.
- DTOs separados de la entidad JPA.
- Credenciales por variables de entorno y secrets.
- Contenedor de ejecución con usuario no root.
- H2 Console deshabilitada en el perfil `prod`.
- Snyk bloquea el pipeline ante vulnerabilidades altas.
- SonarCloud analiza bugs, vulnerabilidades, code smells y duplicación.
- Dependabot mantiene actualizadas las dependencias con revisión mediante PR.
- Política `.snyk` con excepciones justificadas y fecha de expiración.

## Estructura del proyecto

```text
.
├── .github/
│   ├── dependabot.yml
│   └── workflows/
│       └── ci.yml
├── src/
│   ├── main/
│   │   ├── java/com/turnofacil/api/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/java/com/turnofacil/api/
│       ├── controller/
│       ├── exception/
│       └── service/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── pom.xml
└── mvnw
```

## Evidencias

- Pipeline CI/CD: https://github.com/maxrodriguezg/turnofacil-api/actions/workflows/ci.yml
- Dashboard de SonarCloud: https://sonarcloud.io/project/overview?id=maxrodriguezg_turnofacil-api
- Artefacto de cobertura: `reporte-jacoco` en la sección **Artifacts** de cada ejecución.
- Último despliegue validado en `main`: job `Despliegue (Docker Compose)` con healthcheck y smoke test en verde.
- Imágenes publicadas en GHCR: `ghcr.io/maxrodriguezg/turnofacil-api` (tags por SHA y `latest` en `main`).

## Decisiones y limitaciones

- El despliegue es automático pero **simulado** en el runner de GitHub Actions mediante Docker Compose; el entorno se destruye al finalizar la ejecución.
- El pipeline de SonarCloud publica el Quality Gate, pero no bloquea el job; el bloqueo de seguridad lo aplica Snyk.
- Las imágenes base de Docker (`maven` y `eclipse-temurin`) se actualizan manualmente para evitar saltos de JDK incompatibles con Lombok.
- H2 se usa solo en desarrollo; producción usa PostgreSQL.
- La configuración de Dependabot debe permanecer en `main` para que GitHub la tome.

## Reflexiones individuales

### Maximiliano Rodriguez Gamboa


### EA1
> *En este trabajo aprendí como funciona la integración continua trabajando con GitHub actions. Puede entender como y porque debemos automatizar procesos como pruebas y despliegues de una app que estemos desarrollando, es mucho mas eficiente que estar realizando estas pruebas manualmente cada vez que hacemos un cambio, aprendi que es muy importante usar bien los repositorios, y comprendi mucho mejor el manejo de ramas ya que era algo que me costaba mucho, también aprendí a utilizar los Pull Request para proponer y revisar cambios antes de integrarlos a la rama principal, los workflows también fueron importantes para mantener el proyecto mas organizado. Siento que esta experiencia me ayudo a entender mejor como se trabajo con ci/cd en proyextos reales y como puede facilitar el desarrollo y mantenimiento de una app. Con respecto a mi colaboración en el trabajo, me encargue de realizar la presentación, crear el repositorio y sus ramas, e implementar el cambio feat - agrega validación @future en fechaHora para reservas.*

### EA2
> *Durante este trabajo aprendí a integrar SonarCloud, JaCoCo, Snyk y Dependabot en un pipeline CI/CD, comprendiendo la importancia de la calidad, seguridad y trazabilidad del código. Mi contribución fue configurar SonarCloud, resolver conflictos con develop y corregir problemas en los PRs de Dependabot. Las dificultades mas grandes que tuve fueron los errores de autorizacion, la falta de secrets y las incompatibilidades al actualizar el JDK. Si volviera a realizar el trabajo, configuraria las herramientas y los secrets desde el inicio. Esta experiencia me ayudo a comprender como mantener un pipeline más seguro, automatizado y confiable.*

### EP3
> *En esta evaluación aprendí que DevOps no consiste solamente en automatizar despliegues, sino también en monitorear y asegurar el funcionamiento de una aplicación, sobre todo al trabajar con servicios como ECS Fargate y CloudWatch. Mi aporte principal fue participar en la configuración y revisión del monitoreo, incluyendo el dashboard y las alarmas para detectar errores. La dificultad más grande fue lograr que todos los servicios y herramientas funcionaran correctamente, especialmente por los problemas de permisos y las credenciales de AWS Academy, y la resolví revisando las configuraciones y trabajando en equipo para encontrar soluciones. Si volviera a hacerlo, organizaría mejor los despliegues y documentaría cada etapa del proceso, porque eso facilitaría identificar errores y evitaría perder tiempo durante la implementación.*

### Benjamín Dattoli Peña

> *Durante el desarrollo de este proyecto, pude comprender mejor la importancia de implementar prácticas DevOps para garantizar la calidad, seguridad y funcionamiento de una aplicación. La integración de herramientas como GitHub Actions, JaCoCo, SonarCloud, Snyk y Dependabot nos permitió automatizar pruebas, analizar el código e identificar posibles vulnerabilidades en las dependencias utilizadas.
Uno de los principales desafíos fue solucionar los errores que aparecieron durante las ejecuciones del pipeline, especialmente aquellos relacionados con las herramientas de seguridad y las notificaciones automáticas. Esto me permitió aprender a interpretar los registros de ejecución, identificar las causas de los problemas y realizar mejoras sin afectar el funcionamiento general de la API.
Finalmente, este trabajo me permitió reforzar la importancia del trabajo colaborativo, el control de versiones y la integración continua. Comprendí que DevOps no consiste únicamente en automatizar procesos, sino también en mantener un sistema confiable, detectar errores oportunamente y aplicar mejoras constantes durante el desarrollo del software.*

### Vicente Fabar

### EA1
> *Durante este trabajo pude aprender mucho más sobre la integración continua y el uso de GitHub Actions. Entendí mejor la importancia de automatizar tareas como las pruebas y los despliegues, ya que esto permite ahorrar tiempo y evitar tener que realizar estos procesos manualmente cada vez que se hace una modificación en el proyecto. También pude mejorar mi manejo de los repositorios y, especialmente, de las ramas, que era un tema que anteriormente me costaba bastante. Además, aprendí a utilizar los Pull Request para revisar y organizar los cambios antes de incorporarlos a la rama principal, junto con los workflows para mantener un flujo de trabajo más ordenado.*

### EA2
> *en la segunda estapa de este trabajo logre mejorar mis habilidades en devops, debido a que pese ha ya saber de CI/CD en github action, esto hizo que se me aclara varias dudas que tuve, ademas de aprender a desarrollar entre mas personas para la disminucion de errores y crear features para evitar choques entre los trabajos de mis compañeros, las pull request me sirvio para ver que errores podriamos encontrar y evitar para no perjudicar a main (produccion) lo que me facilito bastante el trabajo.*

## Declaración IA

Herramientas: GitHub Copilot, Claude, ChatGPT (GPT-6, High) y opencode. Uso: configuración del pipeline, contenedores y análisis de calidad. Todo el contenido fue revisado y validado con `./mvnw clean verify` y con las ejecuciones de GitHub Actions.

---

**DOY0101 Ingeniería DevOps — Evaluación Parcial 2**  
Repo: `https://github.com/maxrodriguezg/turnofacil-api`