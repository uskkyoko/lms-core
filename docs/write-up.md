# Classboard write-up

Local track. The cluster is kind, three nodes, on this laptop. I did not use AWS. A cloud account would have billed for a load balancer and a managed control plane while I was still finding out why a probe killed the process, and the availability question is the same one either way: when one backend disappears, does the Service keep answering, and for how long is the gap?

The product is defined in [product-definition.md](product-definition.md). The drawing is in [architecture.md](architecture.md). How to run it is in the repository README. How to operate it is in [runbook.md](runbook.md).

## What was reused

The announcement idea comes from Team 1's LMS in this repository (`backend/` and `frontend/`), the web application from the programming course. That system has courses, quizzes, grades, files, and discussions, and it was written by the team. Classboard keeps the operation students actually need when a room or a deadline changes, and it is a new small service. The Java LMS is unchanged. The Dockerfile, Compose file, workflow, Terraform, and manifests were written for this course.

## Source control

Work is on branch `ksenia/classboard`, in small commits. The remote is the shared Team 1 repository, `uskkyoko/lms-core`.

The workflow is `.github/workflows/ci.yml`. It lints, then tests, and only then builds the image. The test job uploads `test-results/junit.xml` with `if: always()`. A push to `main` publishes `ghcr.io/uskkyoko/lms-core/classboard`. The database password in that workflow is for the throwaway Postgres service container. It is not the password of the cluster.

I pushed a commit whose only change was `assert False`, then the commit that removed it.

- Red run, test job failed, image job skipped, and the `test-report` artifact was still uploaded: https://github.com/uskkyoko/lms-core/actions/runs/45803469083
- Green run that removed the failure. Lint, test, and image all succeeded: https://github.com/uskkyoko/lms-core/actions/runs/45803513196

Those runs are on the branch. The entry requirement is a green run on `main`, which happens when the branch is merged. Merging into this repository also changes the LMS the other two people are using, so the pull request needs a real review comment from one of them before that merge. The image build already succeeds on the branch; the push to GHCR is the part that waits for `main`.

No deployed password is in the repository. `TF_VAR_db_password` is an environment variable. The Secret is created by Terraform from it.

## Container

`classboard/Dockerfile` is a two-stage build. The build stage installs `psycopg` into a virtualenv and then deletes pip and setuptools. The final stage is `python:3.12-slim`, user `app` with uid 10001, and the process listens as that user (`docker run --rm --entrypoint id classboard:local` prints `uid=10001`). Alpine is the wrong base here: the psycopg wheel is built for glibc, and compiling it on musl would put a compiler back in the image.

`docker image ls` reports **224MB**. `docker history` splits that into the Debian root (110MB), CPython (45MB), the certificate bundle (5MB), and the virtualenv (16MB). The application files are about 20KB. The size cut that is left is the interpreter itself.

Configuration is environment variables: `PORT`, `APP_VERSION`, `LOG_LEVEL`, and the `DB_*` settings. Nothing in the image points at a database.

`classboard/docker-compose.yml` starts Postgres and the app. Compose waits until Postgres is healthy, then the app becomes healthy on `/ready`. I posted a notice through the page at `http://127.0.0.1:8080` and it showed up at the top of the SE331 list. Filtering to `AI101` showed the empty state, "No notices for AI101 yet."

## Pipeline

`.github/workflows/ci.yml` runs on every push and pull request.

1. `lint` — `ruff check` and `ruff format --check`, with the pip cache.
2. `test` — needs lint. Pytest against a Postgres service container. The JUnit file is uploaded even when the job fails.
3. `image` — needs test. BuildKit cache. Push to GHCR only on a push to `main`.

Locally, before any of that: `ruff check` passed and pytest reported `6 passed, 1 skipped`. The skipped test is the Postgres round trip, and it runs in CI because `DB_HOST` is set there.

## Infrastructure as Code

Two stacks, both applied, both planned again.

- `deploy/terraform/cluster` creates the kind cluster: one control-plane, two workers, host port 30080 mapped to the control-plane.
- `deploy/terraform/platform` creates the namespace, the Secret, and every manifest in `deploy/k8s/`.

kind 0.27 created the cluster and then could not load the image (`unknown containerd config version: 4`). kind 0.33 could. The README requires 0.33 or newer for that reason.

A second `terraform plan` on each stack printed:

```
No changes. Your infrastructure matches the configuration.
```

The output is in `docs/evidence/cluster-plan.txt` and `docs/evidence/platform-plan.txt`.

Teardown, after the measurements below:

```
Destroy complete! Resources: 9 destroyed.
Destroy complete! Resources: 1 destroyed.
```

Nine platform objects, then the cluster. `kind get clusters` then printed `No kind clusters found.` The full log is `docs/evidence/destroy.txt`. Bring it back for the presentation with `./scripts/up.sh`.

## High availability

Two Classboard pods, one on `classboard-worker` and one on `classboard-worker2`. Postgres is one pod, pinned to the control-plane with a toleration, so the drain below does not delete the database. The Service is a NodePort on 30080.

I drained `classboard-worker` while a probe hit `GET /api/announcements?course=SE331` four times a second. From `docs/evidence/failover.txt`:

```
drain_start 2026-11-26T10:31:29Z
pod/classboard-c5b945498-2trwg evicted
drain_done 2026-11-26T10:31:41Z
samples=36 non_200=9
gaps=5 longest_gap_seconds=5.819
```

The pod on `worker2` stayed Ready. Nine probes got no connection, in five bursts, and the longest burst was **5.8 seconds**. That is the time the Service was still aimed at the pod that had just been evicted. After the drain, the same URL returned both notices, including the one posted in the browser. The rows were never on the pod that died.

The spread constraint is `ScheduleAnyway`. I first used `DoNotSchedule`. While the worker was cordoned, the replacement pod stayed Pending: the scheduler still counted the cordoned node, the skew would have been too high, and it refused the only node that was left. Preferring a spread, and still being allowed to stack both pods on the survivor, is the constraint that matches a two-node drain.

## Kubernetes

| Object | Why it is there |
| --- | --- |
| Deployment `classboard`, 2 replicas | The drain above |
| Deployment `postgres`, 1 replica, `Recreate` | One data directory. Two Postgres processes cannot share this PVC |
| Services | App NodePort 30080, Postgres ClusterIP |
| ConfigMap `classboard-config` | Port, version, log level, database host. Not secret |
| Secret `classboard-db` | The password, from Terraform, not from git |
| PVC `postgres-data`, 1Gi | The notices survive a Postgres restart. They do not survive `terraform destroy` |
| PodDisruptionBudget `minAvailable: 1` | A voluntary disruption should leave one pod serving |
| Probes | Liveness `GET /health` does not touch the database. Readiness `GET /ready` runs `SELECT 1` |
| Resources | See below |
| `maxUnavailable: 0`, `maxSurge: 1` | A rollout adds a pod before it removes one |

`/proc/1/status` inside a running pod reported `VmRSS` of 44736 kB, about 44Mi. The request is 64Mi and the limit is 128Mi. The CPU request is 50m and the limit is 250m, so a stuck loop cannot take a whole kind node. Postgres asks for 256Mi and is capped at 512Mi, with `shared_buffers` set to 64MB, because this is a laptop cluster rather than a database server.

The first liveness failure taught me the shutdown path was wrong. The signal handler called `server.shutdown()`, and `shutdown()` waits until `serve_forever` returns. `serve_forever` was running on the same thread as the signal, so it could not return. Kubernetes waited out the grace period and sent SIGKILL. The logs showed `INFO signal=15 shutting down` and then exit 137, and the NodePort started resetting connections. The handler now raises `SystemExit`. A client that has already gone away raises `BrokenPipeError` on write, and that is ignored.

Rolling update, from `docs/evidence/rollout.txt`:

```
rollout_start 2026-11-26T10:32:37Z
{"status": "ok", "version": "2"}
rollback_start 2026-11-26T10:33:55Z
rollback_ready 2026-11-26T10:34:27Z
samples=252 non_200=15
gaps=7 longest_gap_seconds=9.304
```

`kubectl set env` set `APP_VERSION=2`, which changes the pod template, so this was a real rollout rather than a configmap edit the pods would ignore. 237 of 252 probes returned 200. The longest gap was **9.3 seconds**. The curl at the end of the undo still printed version 2, because the previous pod was `Terminating` and still Ready, so the Service had not dropped it yet. After that pod left, the same URL returned `{"status": "ok", "version": "1"}`, and the two new pods were back on the two workers. A second `terraform plan` after the undo was `No changes`, so the live Deployment matched the manifest again.

State lives in Postgres, in the table `announcements`. The app pods write nothing durable. A new pod is Ready only after it can query the database.

## Operations

`GET /health` and `GET /ready` are the probe URLs and the first thing to curl from the laptop. Logs go to stdout. `kubectl logs deploy/classboard` shows lines like `INFO method=GET path=/api/announcements status=200`. The runbook has deploy, rollback, and the three checks.

### Cost

Running this on the laptop costs no cloud account anything, and it is destroyed now. If the same shape ran all month on AWS — a managed Kubernetes control plane, two small worker nodes, a load balancer, and a small database — the list-price order of magnitude is about **$140 a month**: about $73 for the control plane ($0.10 an hour), about $30 for two `t3.small` nodes, about $22 for an application load balancer, and about $15 for a `db.t4g.micro`. The first thing I would cut is the managed control plane. It is more than half of that bill, and this board does not need a control plane I am paying for overnight. A single small VM, or turning the cluster off outside the teaching day, saves more than shrinking the 64Mi app pods.

### Single points of failure

- Postgres is one pod on the control-plane node. Drain that node, or delete the pod and the volume, and `/ready` goes 503 on both app replicas together. The app is highly available. The data is not.
- The kind control plane is one node. It is also where the database runs, and it is the node whose port 30080 is published on the laptop. Losing that container loses the API server, the database, and the host port in one go.
- The PVC is local to this kind cluster. There is no replica and no backup. `terraform destroy` deletes the notices.
- Both replicas read one Secret. A wrong password fails them together.
- The laptop, Docker, and GitHub Actions are outside the cluster. The pipeline being green does not keep the board up.

## What I would show on 3 December

`./scripts/up.sh`, then the page on port 30080, then `./scripts/failover.sh`, then `./scripts/rollout.sh`. The numbers above are what those scripts printed on 26 November. Tear it down again with `./scripts/down.sh` after the session.
