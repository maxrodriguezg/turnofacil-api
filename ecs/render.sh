#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES="$HERE/templates"
OUT="$HERE/out"

export AWS_REGION="${AWS_REGION:-us-east-1}"
export ECS_CLUSTER="${ECS_CLUSTER:-turnofacil-ecs}"
export NAMESPACE="${NAMESPACE:-turnofacil.local}"
export LOG_GROUP="${LOG_GROUP:-/ecs/turnofacil}"
export DB_HOST="${DB_HOST:-db}"
export DB_NAME="${DB_NAME:-turnofacil}"
export DB_USER="${DB_USER:-turno}"
export API_DESIRED_COUNT="${API_DESIRED_COUNT:-2}"

mkdir -p "$OUT"
bash "$HERE/check-vars.sh" "$@"

python3 - "$TEMPLATES" "$OUT" "$@" <<'PY'
import json
import os
import pathlib
import re
import sys

templates = pathlib.Path(sys.argv[1])
out = pathlib.Path(sys.argv[2])
selected = set(sys.argv[3:])
pattern = re.compile(r"\$\{([A-Z0-9_]+)\}")

missing = set()
generated = 0

for tpl in sorted(templates.glob("*.tpl.json")):
    name = tpl.name.removesuffix(".tpl.json")
    if selected and name not in selected:
        continue

    text = tpl.read_text()

    def replace(match: re.Match) -> str:
        key = match.group(1)
        value = os.environ.get(key)
        if value is None:
            missing.add(key)
            return match.group(0)
        return value

    rendered = pattern.sub(replace, text)
    if missing:
        continue

    try:
        json.loads(rendered)
    except json.JSONDecodeError as exc:
        sys.exit(f"JSON invalido en {tpl.name}: {exc}")

    dest = out / f"{name}.json"
    dest.write_text(rendered)
    print(f"generado {dest}")
    generated += 1

if missing:
    sys.exit("Faltan variables: " + ", ".join(sorted(missing)))

if generated == 0:
    sys.exit("No se genero ninguna plantilla. Revisa los nombres indicados.")
PY
