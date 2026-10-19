# LMS Core

Initial prototype of a university Learning Management System. The teaching and learning core is in place: users with roles, courses, and enrollments.

**Backend and frontend both run and talk to each other.** Open the UI at [http://localhost:5173](http://localhost:5173) after starting both services.

## What works now

### Backend (`backend/demo`)

Spring Boot 4 API with JPA and an in-memory H2 database.

- Users: administrator, instructor, student
- Courses: title, unique code, description, term, status, assigned instructors
- Enrollments: students in a course
- Role-based course lists:
  - **Administrator** sees all courses
  - **Instructor** sees courses assigned to them
  - **Student** sees only enrolled courses
- Administrators can create courses and enroll students
- Demo data is loaded on startup: one administrator, one instructor, one student, and two Fall 2026 courses
- Identity for this prototype is the `X-User-Id` header (no login yet)

API (port `8080`):

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/users` | List users (optional `?role=`) |
| `GET` | `/api/me` | Current sample user |
| `GET` | `/api/courses` | Courses visible to the current user |
| `POST` | `/api/courses` | Create a course (admin) |
| `POST` | `/api/courses/{id}/enrollments` | Enroll a student (admin) |

H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

### Frontend (`frontend`)

React + Vite UI that calls the API through a `/api` proxy.

- Student / instructor / administrator dashboards
- Switch role and sample user
- Course list and course page (term, status, instructors, enrolled students)
- Admin forms: create course, enroll student

Not built yet: real authentication, assignments, quizzes, and course materials.

## How to run

You need **Java 17+** and **Node.js**. Start backend first, then frontend.

### 1. Backend

```bash
cd backend/demo
./mvnw spring-boot:run
```

API: [http://localhost:8080](http://localhost:8080)

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: [http://localhost:5173](http://localhost:5173)

Vite proxies `/api` to `http://localhost:8080`. Do not open `frontend/index.html` as a file; use the Vite URL.

## Project layout

```
backend/demo/   Spring Boot API
frontend/       React UI
```
