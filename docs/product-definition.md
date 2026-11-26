# Classboard — product definition

## What it does, and who uses it

Classboard is the notice board for a university's courses. Instructors and administrators post room changes, class cancellations, and deadline extensions. Students read the board before they leave for class or sit down to submit work. The board is the place the university points to when it says "check the course page for changes."

This is a trimmed slice of Team 1's LMS in this repository, the web application built for the programming course (not for this infrastructure course). That LMS also has quizzes, a gradebook, file submissions, and discussions, written across the team. Classboard keeps the announcement operation, because that is the one students look at when something about today's class changes. The Java application is unchanged. The service in `classboard/` is the small deployable form of that operation.

## Core entities

Three entities, stored in one table, `announcements`.

| Entity | What it is here |
| --- | --- |
| Course | The class the notice belongs to, stored as `course_code` (for example `SE331`) |
| Announcement | The notice: a title and a body |
| Author | The staff member who posted it, stored as `author_name` |

A separate users table and a separate courses table are how the full LMS models this. One table is enough to run the board, and it keeps the database story easy to explain.

## What an hour of downtime costs

During exam week an instructor posts that SE331 has moved from one room to another, or that the deadline moved by a day. If the board is down for that hour, students who checked it leave for the old room, or they submit against the old deadline. The ones who did not get the email — and email is how the department falls back — are the ones who miss the change. The university cannot say the course page is the source of truth if the page is the thing that was down. That is the reason for two replicas, a readiness probe, and a Service that does not depend on a single pod.

## Operations this deployment actually implements

1. **Read the board.** `GET /api/announcements?course=SE331` returns the notices for a course, newest first. The home page does this.
2. **Post a notice.** `POST /api/announcements` with a course code, title, body, and author name.
3. **Say whether it can serve.** `GET /health` answers even when the database is down. `GET /ready` answers only when the database accepts a query. Kubernetes uses those two differently, on purpose.
