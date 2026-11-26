#!/usr/bin/env bash
# Build the image, create the kind cluster, load images, apply the platform.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${HOME}/.local/bin:${PATH}"

if [[ -z "${TF_VAR_db_password:-}" ]]; then
  echo "Set TF_VAR_db_password to a local database password." >&2
  exit 1
fi

docker build -t classboard:local "${ROOT}/classboard"
docker pull postgres:16-alpine

terraform -chdir="${ROOT}/deploy/terraform/cluster" init
terraform -chdir="${ROOT}/deploy/terraform/cluster" apply -auto-approve

# kind 0.27 cannot load images into current kind nodes (containerd config v4).
# 0.33 can. Postgres is pulled by the control-plane node; loading that
# multi-arch image with kind trips a containerd import error.
kind load docker-image classboard:local --name classboard

terraform -chdir="${ROOT}/deploy/terraform/platform" init
terraform -chdir="${ROOT}/deploy/terraform/platform" apply -auto-approve

kubectl --context kind-classboard -n classboard rollout status deployment/postgres --timeout=180s
kubectl --context kind-classboard -n classboard rollout restart deployment/classboard
kubectl --context kind-classboard -n classboard rollout status deployment/classboard --timeout=180s

echo "Classboard is at http://127.0.0.1:30080"
