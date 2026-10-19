# University LMS Core — Phase 2 prototype

Spring Boot REST API and React interface for Team 1. The prototype implements two use cases from `ai/SDT213-project-team1.pdf`:

1. **UC-16: Switch role and active user.** Choose Student, Instructor, or Administrator and a predefined database user. The selection is stored in a browser session and survives page refreshes.
2. **UC-12: Course management.** Administrators can create, edit, archive, and delete courses. Required fields and unique course codes are validated on the server. Each course has one or more assigned instructors.

Students see their enrolled courses; instructors see assigned courses. Enrollment management, learning materials, assessments, grades, announcements, and analytics are future work. There is deliberately no login or registration (FR-2). The role switcher is a demonstration mechanism, not production authentication.

## Run locally

Prerequisites: JDK 17+ (tested with JDK 21), Node.js 22.12+, and npm. The Maven wrapper is included; installed Maven 3.9+ also works.

Open two terminals from the repository root.

**Terminal 1 — backend (PowerShell):**

```powershell
cd backend/demo
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, use `./mvnw spring-boot:run`. If Maven is already installed, `mvn spring-boot:run` works on either platform. Wait for `Started DemoApplication` before opening the frontend.

**Terminal 2 — frontend:**

```powershell
cd frontend
npm ci
npm run dev
```

Open **http://127.0.0.1:5173**. Choose a role and name from the top-right header dropdown. On Windows, use `npm.cmd` if PowerShell blocks `npm.ps1`.

The API runs on `127.0.0.1:8080`. Vite proxies `/api` to the backend, including the session cookie. Both servers must be running. Keep the frontend on the same hostname throughout a demo.

The default `local` profile uses a persistent H2 database at `backend/demo/data/lms.mv.db`, so no database installation is needed for an initial demo. Restarting the backend preserves users and courses; select a sample user again after a restart. Sample data is inserted only when the users table is empty. Tests use a separate in-memory database.

## Required project database: MySQL

The `mysql` profile uses the required MySQL database through Spring Data JPA. The local H2 profile is only a convenient prototype option; it does not replace MySQL in the project architecture.

With an existing MySQL server, create an empty `lms_core` database and a user with privileges on that database. In the backend terminal:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://localhost:3306/lms_core'
$env:DB_USERNAME = 'lms'
$env:DB_PASSWORD = 'your-local-database-password'
cd backend/demo
.\mvnw.cmd spring-boot:run
```

Alternatively, with Docker Compose installed, run this from the repository root first:

```powershell
$env:DB_PASSWORD = 'choose-a-local-password'
$env:MYSQL_ROOT_PASSWORD = 'choose-a-different-root-password'
docker compose up -d --wait
```

Then start the backend with the `mysql` profile and the same `DB_PASSWORD`. Compose stores data in the `mysql_data` volume. `docker compose down` stops the database without removing its data. Do not commit passwords. Hibernate creates/updates the prototype tables at startup; versioned migrations remain future work.

To return to the local profile, stop the backend and set `$env:SPRING_PROFILES_ACTIVE = 'local'` before restarting.

## Sample users

| Role | User | Initial course access |
| --- | --- | --- |
| Administrator | Uliana | All courses, with management actions |
| Instructor | Liza | SDT213, SDT205 |
| Student | Kseniia | SDT213 |

Email addresses are demo data. New courses have no students enrolled. Assigning instructors in the course form makes the new course visible to those instructors.

## Verify

Backend tests and executable JAR:

```powershell
cd backend/demo
.\mvnw.cmd clean verify
```

Frontend production build:

```powershell
cd frontend
npm ci
npm run build
```

Browser tests (start the backend first, using the local profile and sample data):

```powershell
cd frontend
npm test
```

The tests use installed Google Chrome and start Vite automatically if needed. If Chrome is unavailable, install it with `npx playwright install chrome`. Browser tests use the real API and database, create a uniquely named course, and clean it up. They expect the original sample courses to exist. API tests use an isolated in-memory database and roll back changes.

The production frontend can be checked locally with `npm run preview` while the backend is running. The packaged backend can be started from `backend/demo` with `java -jar target/demo-0.0.1-SNAPSHOT.jar`.

## Structure

```text
backend/demo/
  src/main/java/com/example/demo/
    user/          User hierarchy, role selection, session handling
    course/        Course and Enrollment entities, REST API, business rules
    SampleData     First-run sample users, courses and enrollments
  src/main/resources/  Local and MySQL configuration
  src/test/            API integration tests and isolated test configuration
frontend/
  src/App.jsx          Role switcher, course directory and course forms
  src/api.js           JSON requests and readable errors
  src/styles.css       Responsive sidebar and course interface
  tests/               Playwright browser scenarios
docs/phase-2-prototype.md  Scope, requirement mapping and presentation checklist
compose.yaml              Optional local MySQL service
```

The existing `backend/demo` directory/package is retained from the original starter. Entities follow the report's User → Student/Instructor/Administrator inheritance, Course–Instructor association, and Student–Enrollment–Course relationship. Controllers handle HTTP, services handle permissions/business operations, and repositories handle persistence. API responses use course DTOs to avoid exposing lazy persistence relationships.

## API

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/api/users?role=STUDENT` | List predefined users, optionally filtered by role |
| PUT | `/api/session` | Select user with `{"userId": 1}`; stores selection in HTTP session |
| GET | `/api/session` | Get active user; 401 when none selected |
| GET | `/api/courses` | List courses visible to the active user |
| POST | `/api/courses` | Create course (Administrator only) |
| PUT | `/api/courses/{id}` | Edit course (Administrator only) |
| PATCH | `/api/courses/{id}/archive` | Archive course (Administrator only) |
| DELETE | `/api/courses/{id}` | Delete course and its enrollments (Administrator only) |

Example course body (obtain instructor IDs from `/api/users?role=INSTRUCTOR`):

```json
{
  "title": "Software Design",
  "code": "SDT301",
  "description": "An introduction to software design.",
  "term": "Fall 2026",
  "instructorIds": [2]
}
```

Preserve the `JSESSIONID` cookie when calling session-dependent endpoints. Course codes are trimmed and stored uppercase; duplicate codes return 409. Invalid values return 400, restricted actions return 403, and missing records return 404. Archived courses remain visible with an Archived label. Deleting a course removes its instructor links and enrollments, but keeps the users.
