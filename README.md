# LMS Core

University LMS teaching and learning core: React frontend, Spring Boot API, H2 database in MySQL mode.

Open the UI at [http://localhost:5173](http://localhost:5173) after starting both services.

## What works now

- Role switcher and sample users (no login; identity is `X-User-Id`)
- Courses, instructor assignment, student enrollment
- Quizzes (FR-21–FR-27): multiple-choice and essay, availability window, time limit with auto-submit, one attempt, autosave, pending grading for essays, review after grading
- Gradebook (FR-28–FR-32): all students and quiz items, totals and percentage, instructor grade changes, immutable history; students see only their own grades

Demo users:

| User | Role | Notes |
| --- | --- | --- |
| Olha Kovalenko | Administrator | all courses |
| Andrii Shevchenko | Instructor | SE331 |
| Ksenia Maranda | Student | SE331 — take a quiz as this user |
| Yelyzaveta Maranda | Student | SE331 — Week 1 graded, Requirements Check pending |
| Uliana Sova | Student | AI101 only |

## How to run

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
