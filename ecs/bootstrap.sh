#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export AWS_REGION="${AWS_REGION:-us-east-1}"
export ECS_CLUSTER="${ECS_CLUSTER:-turnofacil-ecs}"
export NAMESPACE="${NAMESPACE:-turnofacil.local}"
export LOG_GROUP="${LOG_GROUP:-/ecs/turnofacil}"
export DB_NAME="${DB_NAME:-turnofacil}"
export DB_USER="${DB_USER:-turno}"

: "${DB_PASSWORD:?Define DB_PASSWORD antes de ejecutar bootstrap.sh}"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export LAB_ROLE_ARN="${LAB_ROLE_ARN:-arn:aws:iam::${ACCOUNT_ID}:role/LabRole}"

echo "Cuenta: $ACCOUNT_ID | Region: $AWS_REGION | Cluster: $ECS_CLUSTER"

CLUSTER_STATUS=$(aws ecs describe-clusters --clusters "$ECS_CLUSTER" \
  --query "clusters[0].status" --output text 2>/dev/null || true)

if [ "$CLUSTER_STATUS" != "ACTIVE" ]; then
  echo "Creando cluster $ECS_CLUSTER..."
  aws ecs create-cluster --cluster-name "$ECS_CLUSTER" \
    --settings name=containerInsights,value=enabled >/dev/null
else
  echo "Cluster ya existe."
fi

aws ecs update-cluster-settings --cluster "$ECS_CLUSTER" \
  --settings name=containerInsights,value=enabled >/dev/null
echo "Container Insights: enabled"

aws ecs update-cluster --cluster "$ECS_CLUSTER" \
  --service-connect-defaults namespace="$NAMESPACE" >/dev/null
echo "Namespace: $NAMESPACE"

aws logs create-log-group --log-group-name "$LOG_GROUP" 2>/dev/null || true
aws logs put-retention-policy --log-group-name "$LOG_GROUP" --retention-in-days 7
echo "Log group: $LOG_GROUP (retencion 7 dias)"

export VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true \
  --query "Vpcs[0].VpcId" --output text)
export SUBNET1=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" \
  --query "Subnets[0].SubnetId" --output text)
export SUBNET2=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" \
  --query "Subnets[1].SubnetId" --output text)

ensure_sg() {
  local name="$1"
  local description="$2"
  local id
  id=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values="$name" Name=vpc-id,Values="$VPC_ID" \
    --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)
  if [ -z "$id" ] || [ "$id" = "None" ]; then
    id=$(aws ec2 create-security-group --group-name "$name" \
      --description "$description" --vpc-id "$VPC_ID" \
      --query GroupId --output text)
  fi
  printf '%s' "$id"
}

export SG_WEB=$(ensure_sg turnofacil-web-sg "Web publica en 8080")
export SG_API=$(ensure_sg turnofacil-api-sg "API solo accesible desde la web")
export SG_DB=$(ensure_sg turnofacil-db-sg "PostgreSQL solo accesible desde la API")

aws ec2 authorize-security-group-ingress --group-id "$SG_WEB" \
  --protocol tcp --port 8080 --cidr 0.0.0.0/0 >/dev/null 2>&1 || true
aws ec2 authorize-security-group-ingress --group-id "$SG_API" \
  --protocol tcp --port 8080 --source-group "$SG_WEB" >/dev/null 2>&1 || true
aws ec2 authorize-security-group-ingress --group-id "$SG_DB" \
  --protocol tcp --port 5432 --source-group "$SG_API" >/dev/null 2>&1 || true

echo "Security Groups: web=$SG_WEB api=$SG_API db=$SG_DB"

for repo in turnofacil-api turnofacil-web; do
  aws ecr describe-repositories --repository-names "$repo" >/dev/null 2>&1 || \
    aws ecr create-repository --repository-name "$repo" >/dev/null
done
echo "Repositorios ECR listos: turnofacil-api, turnofacil-web"

export SG_DB
bash "$HERE/render.sh" td-db svc-db

DB_TD_ARN=$(aws ecs register-task-definition \
  --cli-input-json "file://$HERE/out/td-db.json" \
  --query "taskDefinition.taskDefinitionArn" --output text)
echo "Task definition DB: $DB_TD_ARN"

DB_STATUS=$(aws ecs describe-services --cluster "$ECS_CLUSTER" --services turnofacil-db \
  --query "services[0].status" --output text 2>/dev/null || true)

if [ "$DB_STATUS" = "ACTIVE" ]; then
  aws ecs update-service --cluster "$ECS_CLUSTER" --service turnofacil-db \
    --task-definition "$DB_TD_ARN" >/dev/null
else
  aws ecs create-service --cli-input-json "file://$HERE/out/svc-db.json" >/dev/null
fi

echo "Esperando a que PostgreSQL quede estable..."
aws ecs wait services-stable --cluster "$ECS_CLUSTER" --services turnofacil-db
echo "PostgreSQL listo."

mkdir -p "$HERE/out"
cat > "$HERE/out/lab.env" <<EOF
export AWS_REGION=$AWS_REGION
export ECS_CLUSTER=$ECS_CLUSTER
export NAMESPACE=$NAMESPACE
export LOG_GROUP=$LOG_GROUP
export VPC_ID=$VPC_ID
export SUBNET1=$SUBNET1
export SUBNET2=$SUBNET2
export SG_WEB=$SG_WEB
export SG_API=$SG_API
export SG_DB=$SG_DB
export LAB_ROLE_ARN=$LAB_ROLE_ARN
export DB_NAME=$DB_NAME
export DB_USER=$DB_USER
EOF
chmod 600 "$HERE/out/lab.env"

cat <<EOF

Bootstrap completado. Variables no secretas guardadas en:
  $HERE/out/lab.env

Carga las variables en tu terminal con:
  source $HERE/out/lab.env
  export DB_PASSWORD='...'

Siguiente paso:
  1) Desplegar la API desde GitHub Actions (Deploy API - ECS Fargate).
  2) Desplegar la web desde GitHub Actions (Deploy Web - ECS Fargate).
EOF
