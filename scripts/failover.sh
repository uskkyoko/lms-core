#!/usr/bin/env bash
# Drain one worker while probing the Service. Then let the node back in.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${ROOT}/docs/evidence/failover.csv"
URL="${1:-http://127.0.0.1:30080/api/announcements?course=SE331}"
mkdir -p "${ROOT}/docs/evidence"

python3 "${ROOT}/scripts/probe.py" "${URL}" "${OUT}" &
PROBE=$!
cleanup() {
  kill "${PROBE}" >/dev/null 2>&1 || true
  wait "${PROBE}" 2>/dev/null || true
}
trap cleanup EXIT

sleep 2
NODE="$(kubectl --context kind-classboard get nodes -l '!node-role.kubernetes.io/control-plane' -o jsonpath='{.items[0].metadata.name}')"
echo "Draining ${NODE}"
date -u +"drain_start %Y-%m-%dT%H:%M:%SZ"
kubectl --context kind-classboard drain "${NODE}" --ignore-daemonsets --delete-emptydir-data --force --grace-period=5 --timeout=180s
date -u +"drain_done %Y-%m-%dT%H:%M:%SZ"
sleep 5
echo "Pods after drain:"
kubectl --context kind-classboard -n classboard get pods -o wide
echo "Uncordoning ${NODE}"
kubectl --context kind-classboard uncordon "${NODE}"
sleep 3
cleanup
trap - EXIT
python3 "${ROOT}/scripts/summarize_gaps.py" "${OUT}"
