# Classboard runbook

The board runs in the `classboard` namespace on the kind cluster `classboard`. From the laptop it is http://127.0.0.1:30080 .

## Deploy

```bash
export TF_VAR_db_password='the-local-password'
./scripts/up.sh
```

That builds `classboard:local`, applies the cluster stack, loads the image into kind, and applies the platform stack. After a code change, run it again. The script restarts the Deployment so the new image is the one that serves.

Watch it come up:

```bash
kubectl --context kind-classboard -n classboard get pods -o wide
kubectl --context kind-classboard -n classboard rollout status deployment/classboard
```

## Roll back

The last applied ReplicaSet is still on the cluster until you delete it.

```bash
kubectl --context kind-classboard -n classboard rollout undo deployment/classboard
kubectl --context kind-classboard -n classboard rollout status deployment/classboard
kubectl --context kind-classboard -n classboard rollout history deployment/classboard
```

`rollout undo` returns the pod template to the previous revision, including the previous `APP_VERSION`. It does not rebuild an image. If the previous image tag is no longer loaded in kind, load it again before you undo.

## First three things to check when it stops

1. **Are the pods up?** `kubectl --context kind-classboard -n classboard get pods -o wide`. If a Classboard pod is `Pending`, read `kubectl describe pod` for a scheduling failure (usually the spread constraint, or a request the node cannot fit). If it is `ImagePullBackOff`, the node does not have `classboard:local`. Run `kind load docker-image classboard:local --name classboard` and delete the pod.
2. **Is it ready, or only alive?** `kubectl --context kind-classboard -n classboard describe pod -l app=classboard`. Liveness (`/health`) can be green while readiness (`/ready`) is red. A red readiness probe means the process is up and the database is not. Then check the Postgres pod and `kubectl --context kind-classboard -n classboard logs deploy/postgres --tail=50`.
3. **What did the process say?** `kubectl --context kind-classboard -n classboard logs deploy/classboard --tail=80`. Request lines look like `INFO method=GET path=/api/announcements status=200`. A line starting `ERROR db` means the password, the host, or the Postgres pod. The password lives in the Secret `classboard-db`, which Terraform created from `TF_VAR_db_password`. It is not in the Git history.

## Logs and the health URL

```bash
curl -sS http://127.0.0.1:30080/health
curl -sS http://127.0.0.1:30080/ready
kubectl --context kind-classboard -n classboard logs -l app=classboard --tail=50
```

## Stop it

```bash
export TF_VAR_db_password='the-same-password'
./scripts/down.sh
```

Destroy the platform stack first. `scripts/down.sh` does that, then destroys the cluster.
