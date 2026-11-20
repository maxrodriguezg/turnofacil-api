#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" != "--yes" ]; then
  echo "Este script elimina servicios, cluster, ECR, logs, SNS, namespace y Security Groups."
  echo "Ejecuta: bash ecs/teardown.sh --yes"
  exit 1
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export AWS_REGION="${AWS_REGION:-us-east-1}"
export ECS_CLUSTER="${ECS_CLUSTER:-turnofacil-ecs}"

NS_ARN=$(aws ecs describe-clusters --clusters "$ECS_CLUSTER" \
  --query "clusters[0].serviceConnectDefaults.namespace" --output text 2>/dev/null || true)
NS_ID="${NS_ARN##*/}"

echo "Eliminando servicios..."
for service in turnofacil-web turnofacil-api turnofacil-db; do
  aws ecs delete-service --cluster "$ECS_CLUSTER" --service "$service" --force >/dev/null 2>&1 || true
done
aws ecs wait services-inactive --cluster "$ECS_CLUSTER" \
  --services turnofacil-web turnofacil-api turnofacil-db 2>/dev/null || true

echo "Eliminando cluster..."
aws ecs delete-cluster --cluster "$ECS_CLUSTER" >/dev/null 2>&1 || true

echo "Eliminando topic SNS..."
TOPIC_ARN=$(aws sns list-topics --region "$AWS_REGION" \
  --query "Topics[?contains(TopicArn,'turnofacil-mesh-alertas')].TopicArn | [0]" --output text 2>/dev/null || true)
if [ -n "$TOPIC_ARN" ] && [ "$TOPIC_ARN" != "None" ]; then
  aws sns delete-topic --topic-arn "$TOPIC_ARN" >/dev/null 2>&1 || true
fi

echo "Eliminando log groups..."
aws logs delete-log-group --log-group-name /ecs/turnofacil >/dev/null 2>&1 || true
aws logs delete-log-group \
  --log-group-name /aws/ecs/containerinsights/turnofacil-ecs/performance >/dev/null 2>&1 || true

echo "Eliminando repositorios ECR..."
for repo in turnofacil-api turnofacil-web; do
  aws ecr delete-repository --repository-name "$repo" --force >/dev/null 2>&1 || true
done

echo "Eliminando namespace..."
if [ -n "$NS_ID" ] && [ "$NS_ID" != "None" ]; then
  aws servicediscovery delete-namespace --id "$NS_ID" >/dev/null 2>&1 || \
    echo "  (namespace no eliminado: sin permisos en Academy; no genera costo)"
fi

echo "Desregistrando task definitions..."
for family in turnofacil-db turnofacil-api turnofacil-web; do
  for arn in $(aws ecs list-task-definitions --family-prefix "$family" \
    --query "taskDefinitionArns[]" --output text 2>/dev/null || true); do
    aws ecs deregister-task-definition --task-definition "$arn" >/dev/null 2>&1 || true
  done
done

echo "Eliminando Security Groups (esperando liberacion de ENI)..."
sleep 60
export VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true \
  --query "Vpcs[0].VpcId" --output text)
for name in turnofacil-web-sg turnofacil-api-sg turnofacil-db-sg; do
  id=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values="$name" Name=vpc-id,Values="$VPC_ID" \
    --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)
  if [ -n "$id" ] && [ "$id" != "None" ]; then
    aws ec2 delete-security-group --group-id "$id" >/dev/null 2>&1 || \
      echo "  No se pudo eliminar $name (aun en uso)."
  fi
done

rm -rf "$HERE/out"
echo "Limpieza terminada."
