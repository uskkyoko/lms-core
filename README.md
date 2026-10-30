# LMS Core

React frontend and Spring Boot REST API for Team 1's university LMS. MySQL is the project database; a persistent H2 profile is available for local development.

## Implemented functionality

- Three sample roles and active-user selection, retained across page refreshes. No login or registration (FR-1-4).
- Administrator course creation, editing, archiving and deletion, required descriptions, unique normalized codes and one or more assigned instructors (FR-5-8).
- Instructor assignment, student enrollment, duplicate prevention and removal. Removed enrollments retain their original date and row; course and quiz/grade access are revoked immediately (FR-7/10, NFR-2).
- Shared `User` inheritance (`Student`, `Instructor`, `Administrator`), `Course`, `Enrollment` and ordered `Module` entities.
- Existing quiz and gradebook workflows, including pending essays and grade history.
- Student Progress and Instructor Analytics using quizzes, assignment submissions, and authoritative grades from the shared `Grade` row. Grade changes are reflected when reports refresh.
- Course content: modules, text pages, video embeds or files, and TXT/DOCX/PDF materials that can be published or hidden.
- Assignments on a module, with file submission and replacement until the server-side deadline, then instructor scoring and feedback on the shared `Grade` record.
- Announcements, plus discussion posts and replies with author edit rights and instructor or administrator delete rights.

See [Uliana's implementation and integration guide](docs/uliana-phase-3-implementation.md) for API contracts, test mapping, deletion policy and remaining dependencies. The [alignment review](docs/phase-3-alignment-review.md) records the state before these implementation changes.

## Run locally

Requires JDK 17+ and Node.js/npm. Open two terminals from the repository root.

Backend (PowerShell):

```powershell
cd backend/demo
.\mvnw.cmd spring-boot:run
```

If Maven is already installed, `mvn spring-boot:run` also works. On macOS/Linux use `./mvnw spring-boot:run`.

Frontend:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies `/api` to the backend at `localhost:8080`. Both servers are needed. On macOS/Linux use `npm` in place of `npm.cmd`. The API accepts the active demo identity in `X-User-Id`; server-side checks resolve that ID from the database. This is a demonstration identity mechanism, not production authentication.

The default `local` profile stores H2 data at `backend/demo/data/lms.mv.db`. Restarting preserves records. Seed data is inserted only into a database with no users. Tests use isolated in-memory databases, never the developer's saved database.

## MySQL

Create an empty `lms_core` database and an application user with privileges on that database, or start the optional Compose service from the repository root:

```powershell
$env:DB_PASSWORD = 'choose-a-local-password'
$env:MYSQL_ROOT_PASSWORD = 'choose-a-different-root-password'
docker compose up -d --wait
```

Then start the backend:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://localhost:3306/lms_core'
$env:DB_USERNAME = 'lms'
$env:DB_PASSWORD = 'your-local-password'
cd backend/demo
.\mvnw.cmd spring-boot:run
```

Use the same application password as Compose when using it. Do not commit credentials. `docker compose down` stops the service without removing its data volume. Hibernate uses `ddl-auto=update` for this coursework; back up an existing database before upgrading. Versioned production migrations are not included.

CORS permits `http://localhost:5173` and `http://127.0.0.1:5173`. Other frontend deployments can set the `lms.allowed-origins` Spring property to a comma-separated list of their explicit origins. Vite's backend target can be changed with `LMS_API_TARGET`.

## Sample users

| User | Role | Seeded course access |
| --- | --- | --- |
| Olha Kovalenko | Administrator | All courses and management controls |
| Andrii Shevchenko | Instructor | SE331 and AI101 |
| Ksenia Maranda | Student | SE331; available quiz attempts |
| Yelyzaveta Maranda | Student | SE331; one graded quiz and one pending essay quiz |
| Uliana Sova | Student | AI101 only |

Demo identities are independent of each team member's development responsibilities. Choose **Administrator** to demonstrate Admin work. Choose **Student / Yelyzaveta Maranda** and **View progress** to see seeded activity; choose **Instructor**, select SE331 and **View analytics** to see submission rates and missing work.

## Verification

```powershell
cd backend/demo
.\mvnw.cmd clean verify
```

```powershell
cd frontend
npm.cmd ci
npm.cmd run build
npm.cmd test
```

Browser tests require installed Google Chrome and the packaged backend JAR from `clean verify`. They automatically launch the API on port 18080 with an isolated in-memory database and Vite on port 15173. They do not use the local development database. Their screenshots and traces are written to ignored `frontend/test-results`. Playwright reports are ignored too.

Verified on 30 October 2026: 29 backend tests passed on both H2 and an isolated MySQL 8.0.41 database; three Chrome scenarios passed; production build passed. Full material/submission acceptance for TC-30/31 still depends on the other team members' modules.