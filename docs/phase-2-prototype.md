# Phase 2 prototype: scope and demonstration

Source of scope: `ai/SDT213-project-team1.pdf` (requirements, use cases, CRC cards, class diagram, UI mockups, and test cases); delivery scope checked against `ai/Phase 2.pdf` and `ai/Project Overview-3.pdf`. The source PDFs remain unchanged and ignored by Git as configured in the repository.

## Selected functionalities

**UC-16 — Switch role and active user:** the smallest independent functionality, and a prerequisite for showing any other role-specific workflow. Implements FR-1 through FR-4. Database-backed sample users are selected through the React interface, with the active ID stored in the browser's server-side session. Different browser sessions do not share the active user.

**UC-12 — Course management:** a small, complete CRUD workflow without file handling, timers, grading, or external integrations. Covers the main create flow and the edit/archive/delete alternatives. Implements FR-5 and FR-8, including validation, case-insensitive unique course codes, and one or more instructors selected when creating/editing a course. Backend role checks protect all changes.

Supporting behavior: seeded Enrollment records demonstrate FR-10 and role-specific dashboards. The course form supports instructor associations needed by FR-8; a standalone UC-14 assignment screen and UC-13 enrollment management are not implemented.

## Traceability

| Report test | Expected behavior | Verification |
| --- | --- | --- |
| TC-01 | Select a sample student and see their view | API and browser tests |
| TC-02 | Select a sample instructor and see their view | API and browser tests |
| TC-03 | Select administrator and see management actions | API and browser tests |
| TC-04 | Create a course with required details and instructor | API and browser tests |
| TC-05 | Prevent duplicate course codes | API and browser tests, including lowercase duplicate |
| TC-08 | Show only the student's enrolled courses | API and browser tests with one enrolled student |
| TC-32 | Edit, archive, and delete courses | API and browser tests |
| UC-12 A2 | Reject missing information without saving | API tests and required form fields |
| FR-5 / NFR-2 | Prevent Student/Instructor course mutations | API tests for POST, PUT, PATCH, DELETE |

Additional checks cover independent sessions, invalid user selection, invalid instructor selection, empty course lists, page refresh, cancellation of deletion, backend connection errors, and a narrow viewport. These checks validate this prototype's scope; they do not establish completion of the full report's test plan.

## Five-minute presentation

1. Start Spring Boot and React using the README. Open `http://127.0.0.1:5173`.
2. Select **Administrator - Uliana** in the top-right header. Show the two sample courses.
3. Click **Create course**. Enter a title, a unique code such as `DEMO301`, a description and a term. Select Liza and save. The new course appears immediately.
4. Try creating another course with code `demo301`. Show the duplicate-code error and cancel.
5. Edit the new course's description. Refresh the page and show that the change remains.
6. Switch to **Instructor → Liza**. Show the assigned course and the absence of management actions.
7. Switch to **Student → Kseniia**. Only SDT213 appears. New courses are not automatically enrolled.
8. Return to Administrator. Archive the new course, filter to Archived, then delete it using the confirmation dialog.
9. Show passing backend and browser test output.

## Prototype boundaries

- The default local profile uses a file-based H2 database; a separate MySQL profile and optional Compose service are supplied for the required stack.
- Sample identities are intentionally selectable without login, as required by FR-2. This local prototype is not a production authentication system.
- Archived courses remain visible and labelled. Reactivation is not part of the selected use case.
- Course deletion removes enrollment and instructor associations. Future content entities will need an explicit deletion policy when implemented.
- Existing source classes retain the starter package `com.example.demo`; only the subset of UML entities needed for these workflows is implemented.
- Session selection is lost when the backend restarts; stored users and courses remain.
- The remaining LMS functionality and the rest of the Phase 2 report deliverables are not implemented by this prototype.

## Validation environment

Backend: Java 21, Spring Boot 4.1.1, Maven 3.9.9, H2. Frontend: Node 22.14, React 19, Vite 7, Playwright with Google Chrome. MySQL configuration is supplied but requires a running MySQL instance for separate verification.

Verified on October 19, 2026:

- Maven package: 8 tests passed (7 API integration tests and 1 application-context test).
- React production build completed successfully.
- All 3 Playwright scenarios passed against the running Spring Boot API.
- Desktop and 390-pixel mobile screenshots reviewed; no horizontal overflow.
- A temporary course created through the API survived a full backend stop/restart. The test record was then removed.
- MySQL execution was not tested in this environment.
