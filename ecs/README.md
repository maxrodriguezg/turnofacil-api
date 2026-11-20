# Infraestructura ECS de TurnoFácil (API + PostgreSQL)

Esta carpeta contiene las plantillas y scripts para desplegar la API y PostgreSQL en **ECS Fargate con Service Connect**, con trazas hacia **AWS X-Ray** usando **ADOT/OpenTelemetry**.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `templates/td-db.tpl.json` | Task definition de PostgreSQL 16 |
| `templates/svc-db.tpl.json` | Servicio `turnofacil-db` con alias `db:5432` |
| `templates/td-api.tpl.json` | Task definition de la API + sidecar `otel-collector` |
| `templates/svc-api.tpl.json` | Servicio `turnofacil-api` con 2 réplicas |
| `render.sh` | Reemplaza `${VARIABLES}` y valida el JSON resultante |
| `check-vars.sh` | Revisa herramientas y variables antes de renderizar |
| `bootstrap.sh` | Crea clúster, namespace, logs, red, SGs, ECR y PostgreSQL |
| `teardown.sh` | Elimina los recursos para detener el cobro |
| `.github/workflows/bootstrap-ecs.yml` | Ejecuta `bootstrap.sh` manualmente desde Actions |

## Flujo recomendado

### 1. Bootstrap (una vez por cuenta/sesión)

Desde GitHub Actions: **Actions → Bootstrap ECS → Run workflow**.

O desde la terminal:

```bash
export DB_PASSWORD='...'
bash ecs/bootstrap.sh
source ecs/out/lab.env
export DB_PASSWORD='...'
```

Crea:

- clúster `turnofacil-ecs` con Container Insights;
- namespace `turnofacil.local`;
- log group `/ecs/turnofacil` con retención 7 días;
- VPC/subredes por defecto;
- Security Groups `turnofacil-web-sg`, `turnofacil-api-sg`, `turnofacil-db-sg`;
- repositorios ECR;
- servicio PostgreSQL 1/1.

### 2. Desplegar la API

Desde GitHub Actions: **Actions → Deploy API - ECS Fargate → Run workflow**.

El workflow:

1. construye la imagen y la sube a ECR con el SHA del commit;
2. renderiza `td-api.json` y `svc-api.json`;
3. registra la task definition;
4. crea o actualiza el servicio;
5. espera `services-stable`.

### 3. Desplegar la web

Desde el repo `turnofacil-web`: **Actions → Deploy Web - ECS Fargate → Run workflow**.

> La API debe estar activa antes de desplegar la web, porque Nginx resuelve `turnofacil-api` al arrancar.

## Variables y secretos de GitHub

### Secrets (repositorio)

| Nombre | Uso |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credenciales AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Credenciales AWS Academy |
| `AWS_SESSION_TOKEN` | Token de sesión de Academy |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL (se pasa como `DB_PASSWORD` a los scripts) |

### Variables (repository variables, opcionales)

> Los secrets son obligatorios. Las variables de red (`SUBNET1`, `SUBNET2`, `SG_API`, `LAB_ROLE_ARN`) son opcionales: el workflow las descubre por nombre si no están definidas.

| Nombre | Ejemplo |
|---|---|
| `AWS_REGION` | `us-east-1` |
| `ECS_CLUSTER` | `turnofacil-ecs` |
| `NAMESPACE` | `turnofacil.local` |
| `LOG_GROUP` | `/ecs/turnofacil` |
| `SUBNET1` | `subnet-...` |
| `SUBNET2` | `subnet-...` |
| `SG_API` | `sg-...` |
| `SG_WEB` | `sg-...` (solo repo web) |
| `LAB_ROLE_ARN` | `arn:aws:iam::<cuenta>:role/LabRole` |
| `DB_NAME` | `turnofacil` |
| `DB_USER` | `turno` |

> Las credenciales de AWS Academy **caducan**. Actualiza los tres secrets antes de cada sesión de despliegue.

## Trazas con X-Ray

- La imagen de la API incluye el **ADOT Java agent** (`/opt/aws-opentelemetry-agent.jar`).
- La task definition arranca el agente con `JAVA_TOOL_OPTIONS=-javaagent:...` y envía trazas OTLP a `localhost:4317`.
- El sidecar `otel-collector` recibe OTLP y exporta a **X-Ray** (`awsxray` exporter).
- La task definition usa `taskRoleArn=LabRole` para que el collector obtenga permisos de X-Ray.

Verificar trazas:

```bash
aws xray get-trace-summaries \
  --start-time $(date -u -v-5M +%s) \
  --end-time $(date -u +%s) \
  --region us-east-1 \
  --query "TraceSummaries[*].{ID:Id,Duracion:Duration,HTTP:Http.HttpStatus,URL:Http.HttpURL}" \
  --output table
```

Si no aparecen:

- revisa los logs del contenedor `otel` en `/ecs/turnofacil`;
- confirma que `LabRole` tenga permisos `xray:PutTraceSegments` y `xray:PutTelemetryRecords`;
- recuerda que el agente se activa recién con la imagen nueva (hay que reconstruir y desplegar).

## Render local (sin desplegar)

```bash
export LAB_ROLE_ARN=arn:aws:iam::<cuenta>:role/LabRole
export DB_PASSWORD='...'
export API_IMAGE_URI=<cuenta>.dkr.ecr.us-east-1.amazonaws.com/turnofacil-api:lab
export SUBNET1=subnet-...
export SUBNET2=subnet-...
export SG_API=sg-...

bash ecs/render.sh
ls ecs/out/
```

`ecs/out/` está en `.gitignore`.

## Notas y limitaciones

- La base de datos usa almacenamiento efímero: al reemplazar la tarea se pierden los datos. La API recrea el esquema con Hibernate (`ddl-auto=update`).
- Si un servicio quedó `INACTIVE`, AWS puede tardar en liberar el nombre; si `create-service` falla con `ServiceAlreadyExistsException`, espera unos minutos o cambia temporalmente el nombre.
- Las credenciales de Academy expiran; el despliegue desde GitHub Actions solo funciona mientras estén vigentes.
- La imagen del collector usa `:latest`. Para producción, fija una versión inmutable.
- El agente ADOT está fijado en `v2.33.0` en el `Dockerfile`.
- El laboratorio no usa ALB: cada tarea tiene IP pública, pero solo la web tiene el puerto 8080 abierto.
