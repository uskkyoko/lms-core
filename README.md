# Classboard

Classboard is the course announcement board for a university. Instructors post room changes, cancellations, and deadline extensions. Students read them before they leave for class. The full Team 1 LMS in this repository does much more (quizzes, grades, files, discussions). This deployment keeps the one operation that has to stay up during the hour a change goes out.

Track: **local**. A three-node [kind](https://kind.sigs.k8s.io/) cluster on this machine. Nothing here is billed by a cloud account. The availability question is the same as in the cloud track: traffic has to keep flowing when one backend disappears.

## What a clean machine needs

- Docker
- [kind](https://kind.sigs.k8s.io/) 0.33 or newer (`kind` on your `PATH`). 0.27 creates the cluster and then fails to load images, because it does not understand the containerd config these nodes use.
- kubectl
- Terraform 1.5 or newer
- A free TCP port 30080
- Python 3.12 if you want to run the tests outside Docker. The running service does not need a host Python.

## Run the board and its database

From `classboard/`:

```bash
cp .env.example .env
# edit .env and set DB_PASSWORD to something that is not a password you use anywhere else
docker compose up --build
```

Open http://127.0.0.1:8080 . `docker compose down` stops it. Add `-v` to delete the database volume too.

## Run it on Kubernetes

The cluster and every Kubernetes object come from Terraform. The database password is not in the repository.

```bash
export TF_VAR_db_password='pick-a-local-password'
./scripts/up.sh
```

Open http://127.0.0.1:30080 .

`./scripts/up.sh` builds `classboard:local`, creates a kind cluster with one control-plane and two workers, loads the images into the nodes, and applies the manifests. Run it again after a code change; it restarts the Deployment so the nodes pick up the new image.

Tear it down when you are finished. Nothing should be left running:

```bash
export TF_VAR_db_password='the-same-password'
./scripts/down.sh
```

## Checks, rollback, and the pipeline

Operational steps live in [docs/runbook.md](docs/runbook.md). The product page is [docs/product-definition.md](docs/product-definition.md). The architecture is [docs/architecture.md](docs/architecture.md).

GitHub Actions (`.github/workflows/ci.yml`) lints, tests, then builds the image. A push to `main` also publishes it to `ghcr.io/<owner>/lms-core/classboard`.

## Repository map

| Path | What it is |
| --- | --- |
| `classboard/` | The service, Dockerfile, and Compose file |
| `deploy/k8s/` | Kubernetes manifests |
| `deploy/terraform/cluster` | kind cluster |
| `deploy/terraform/platform` | Namespace, Secret, and manifests |
| `.github/workflows/ci.yml` | Lint, test, image |
| `backend/`, `frontend/` | The original Team 1 LMS. Not what this deployment runs |

## Original LMS application

The Java and React LMS below is the coursework this board was trimmed from. It is not started by the commands above.

React frontend and Spring Boot REST API for Team 1's university LMS. MySQL is the project database; a persistent H2 profile is available for local development.

### Implemented functionality

- Three sample roles and active-user selection, retained across page refreshes. No login or registration (FR-1-4).
- Administrator course creation, editing, archiving and deletion, required descriptions, unique normalized codes and one or more assigned instructors (FR-5-8).
- Instructor assignment, student enrollment, duplicate prevention and removal. Removed enrollments retain their original date and row; course and quiz/grade access are revoked immediately (FR-7/10, NFR-2).
- Shared `User` inheritance (`Student`, `Instructor`, `Administrator`), `Course`, `Enrollment` and ordered `Module` entities.
- Existing quiz and gradebook workflows, including pending essays and grade history.
- Student Progress and Instructor Analytics using quizzes, assignment submissions, and authoritative grades from the shared `Grade` row. Grade changes are reflected when reports refresh.
- Course content: modules, text pages, video embeds or files, and TXT/DOCX/PDF materials that can be published or hidden.
- Assignments on a module, with file submission and replacement until the server-side deadline, then instructor scoring and feedback on the shared `Grade` record.
- Announcements, plus discussion posts and replies with author edit rights and instructor or administrator delete rights.

### Run the original LMS locally

Requires JDK 17+ and Node.js/npm. Open two terminals from the repository root.

Backend:

```bash
cd backend/demo
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173 . Vite proxies `/api` to the backend at `localhost:8080`. The API accepts the active demo identity in `X-User-Id`. This is a demonstration identity mechanism, not production authentication.

The default `local` profile stores H2 data at `backend/demo/data/lms.mv.db`. Tests use isolated in-memory databases.

MySQL for that LMS, if you need it, is the Compose file at the repository root (`compose.yaml`), which is separate from `classboard/docker-compose.yml`.
