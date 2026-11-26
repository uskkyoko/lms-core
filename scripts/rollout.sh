#!/usr/bin/env bash
# Roll the app forward and back while probing. Requests should keep returning 200.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${ROOT}/docs/evidence/rollout.csv"
URL="${1:-http://127.0.0.1:30080/health}"
mkdir -p "${ROOT}/docs/evidence"
CTX=(kubectl --context kind-classboard -n classboard)

python3 "${ROOT}/scripts/probe.py" "${URL}" "${OUT}" &
PROBE=$!
cleanup() {
  kill "${PROBE}" >/dev/null 2>&1 || true
  wait "${PROBE}" 2>/dev/null || true
}
trap cleanup EXIT

sleep 2
echo "Rolling APP_VERSION to 2"
date -u +"rollout_start %Y-%m-%dT%H:%M:%SZ"
"${CTX[@]}" set env deployment/classboard APP_VERSION=2
"${CTX[@]}" rollout status deployment/classboard --timeout=180s
date -u +"rollout_ready %Y-%m-%dT%H:%M:%SZ"
# rollout status returns while the previous pod can still answer.
# Give it a few seconds to leave the endpoints, then read the version.
sleep 5
curl -fsS http://127.0.0.1:30080/health
echo
echo "Rolling back"
date -u +"rollback_start %Y-%m-%dT%H:%M:%SZ"
"${CTX[@]}" rollout undo deployment/classboard
"${CTX[@]}" rollout status deployment/classboard --timeout=180s
date -u +"rollback_ready %Y-%m-%dT%H:%M:%SZ"
sleep 5
curl -fsS http://127.0.0.1:30080/health
echo
sleep 2
cleanup
trap - EXIT
python3 "${ROOT}/scripts/summarize_gaps.py" "${OUT}"
