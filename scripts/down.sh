#!/usr/bin/env bash
# Destroy platform resources first so Terraform can still talk to the API server.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${HOME}/.local/bin:${PATH}"

if [[ -z "${TF_VAR_db_password:-}" ]]; then
  echo "Set TF_VAR_db_password. It must match the password used at apply time." >&2
  exit 1
fi

terraform -chdir="${ROOT}/deploy/terraform/platform" destroy -auto-approve
terraform -chdir="${ROOT}/deploy/terraform/cluster" destroy -auto-approve
echo "Cluster destroyed."
