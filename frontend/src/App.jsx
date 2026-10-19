import { useEffect, useMemo, useState } from "react";
import { createCourse, enrollStudent, fetchCourses, fetchUsers } from "./api.js";

const ROLES = [
  { value: "STUDENT", label: "Student" },
  { value: "INSTRUCTOR", label: "Instructor" },
  { value: "ADMINISTRATOR", label: "Administrator" },
];

const STORAGE_KEY = "lms-active-user-id";

export default function App() {
  const [users, setUsers] = useState([]);
  const [activeUserId, setActiveUserId] = useState(() => localStorage.getItem(STORAGE_KEY) || "");
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState({ title: "", code: "", description: "", term: "Fall 2026" });
  const [enrollStudentId, setEnrollStudentId] = useState("");

  const activeUser = users.find((user) => String(user.id) === String(activeUserId));
  const roleUsers = useMemo(
    () => users.filter((user) => user.role === (activeUser?.role || "STUDENT")),
    [users, activeUser]
  );
  const students = users.filter((user) => user.role === "STUDENT");
  const selectedCourse = courses.find((course) => course.id === selectedCourseId) || courses[0];

  useEffect(() => {
    fetchUsers()
      .then((data) => {
        setUsers(data);
        if (!activeUserId && data.length) {
          const firstStudent = data.find((user) => user.role === "STUDENT") || data[0];
          setActiveUserId(String(firstStudent.id));
        }
      })
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (!activeUserId) {
      return;
    }
    localStorage.setItem(STORAGE_KEY, String(activeUserId));
    fetchCourses(activeUserId)
      .then((data) => {
        setCourses(data);
        setSelectedCourseId((current) =>
          data.some((course) => course.id === current) ? current : data[0]?.id || null
        );
        setError("");
      })
      .catch((err) => setError(err.message));
  }, [activeUserId]);

  function switchRole(role) {
    const next = users.find((user) => user.role === role);
    if (next) {
      setActiveUserId(String(next.id));
      setNotice(`Switched to ${role.toLowerCase()} view.`);
    }
  }

  async function onCreateCourse(event) {
    event.preventDefault();
    setError("");
    setNotice("");
    try {
      const created = await createCourse(activeUserId, form);
      setCourses((current) => [...current, created].sort((a, b) => a.code.localeCompare(b.code)));
      setSelectedCourseId(created.id);
      setForm({ title: "", code: "", description: "", term: "Fall 2026" });
      setNotice(`Course ${created.code} created.`);
    } catch (err) {
      setError(err.message);
    }
  }

  async function onEnroll(event) {
    event.preventDefault();
    if (!selectedCourse || !enrollStudentId) {
      return;
    }
    setError("");
    setNotice("");
    try {
      const updated = await enrollStudent(activeUserId, selectedCourse.id, Number(enrollStudentId));
      setCourses((current) => current.map((course) => (course.id === updated.id ? updated : course)));
      setEnrollStudentId("");
      setNotice("Student enrolled.");
    } catch (err) {
      setError(err.message);
    }
  }

  const isAdmin = activeUser?.role === "ADMINISTRATOR";

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">AUK</span>
          <div>
            <strong>University LMS</strong>
            <p>Teaching &amp; learning core</p>
          </div>
        </div>
        <nav>
          <button className="nav-item active" type="button">
            Dashboard
          </button>
          <button className="nav-item" type="button">
            My Courses
          </button>
        </nav>
      </aside>

      <main>
        <header className="topbar">
          <div>
            <h1>
              {activeUser?.role === "ADMINISTRATOR"
                ? "Administrator Dashboard"
                : activeUser?.role === "INSTRUCTOR"
                  ? "Instructor Dashboard"
                  : "Student Dashboard"}
            </h1>
            <p className="muted">
              {activeUser
                ? `${activeUser.fullName} · ${activeUser.role.toLowerCase()}`
                : "Select a sample user"}
            </p>
          </div>
          <div className="role-switcher">
            <label>
              Role
              <select
                value={activeUser?.role || "STUDENT"}
                onChange={(event) => switchRole(event.target.value)}
              >
                {ROLES.map((role) => (
                  <option key={role.value} value={role.value}>
                    {role.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Sample user
              <select value={activeUserId} onChange={(event) => setActiveUserId(event.target.value)}>
                {roleUsers.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.fullName}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </header>

        {error ? <div className="banner error">{error}</div> : null}
        {notice ? <div className="banner success">{notice}</div> : null}

        <section className="grid">
          <div>
            <h2>Available courses</h2>
            <p className="muted">
              {activeUser?.role === "STUDENT" && "Only courses you are enrolled in."}
              {activeUser?.role === "INSTRUCTOR" && "Courses assigned to you."}
              {activeUser?.role === "ADMINISTRATOR" && "All courses in the system."}
            </p>
            <div className="cards">
              {courses.length === 0 ? (
                <div className="empty">No courses for this user yet.</div>
              ) : (
                courses.map((course) => (
                  <button
                    key={course.id}
                    type="button"
                    className={`card ${selectedCourse?.id === course.id ? "selected" : ""}`}
                    onClick={() => setSelectedCourseId(course.id)}
                  >
                    <span className="code">{course.code}</span>
                    <strong>{course.title}</strong>
                    <span className="muted">{course.term}</span>
                  </button>
                ))
              )}
            </div>
          </div>

          <div>
            {selectedCourse ? (
              <article className="panel">
                <h2>
                  {selectedCourse.code} – {selectedCourse.title}
                </h2>
                <p>{selectedCourse.description}</p>
                <dl>
                  <div>
                    <dt>Term</dt>
                    <dd>{selectedCourse.term}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>{selectedCourse.status}</dd>
                  </div>
                  <div>
                    <dt>Instructors</dt>
                    <dd>
                      {selectedCourse.instructors.length
                        ? selectedCourse.instructors.map((user) => user.fullName).join(", ")
                        : "Not assigned"}
                    </dd>
                  </div>
                  <div>
                    <dt>Enrolled students</dt>
                    <dd>
                      {selectedCourse.students.length
                        ? selectedCourse.students.map((user) => user.fullName).join(", ")
                        : "None"}
                    </dd>
                  </div>
                </dl>
              </article>
            ) : (
              <article className="panel">
                <h2>Course page</h2>
                <p className="muted">Select a course to see details.</p>
              </article>
            )}

            {isAdmin ? (
              <>
                <form className="panel" onSubmit={onCreateCourse}>
                  <h2>Create course</h2>
                  <label>
                    Title
                    <input
                      value={form.title}
                      onChange={(event) => setForm({ ...form, title: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Course code
                    <input
                      value={form.code}
                      onChange={(event) => setForm({ ...form, code: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Term
                    <input
                      value={form.term}
                      onChange={(event) => setForm({ ...form, term: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Description
                    <textarea
                      value={form.description}
                      onChange={(event) => setForm({ ...form, description: event.target.value })}
                    />
                  </label>
                  <button className="primary" type="submit">
                    Save course
                  </button>
                </form>

                {selectedCourse ? (
                  <form className="panel" onSubmit={onEnroll}>
                    <h2>Enroll student</h2>
                    <label>
                      Student
                      <select
                        value={enrollStudentId}
                        onChange={(event) => setEnrollStudentId(event.target.value)}
                        required
                      >
                        <option value="">Select a student</option>
                        {students.map((student) => (
                          <option key={student.id} value={student.id}>
                            {student.fullName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button className="primary" type="submit">
                      Enroll
                    </button>
                  </form>
                ) : null}
              </>
            ) : null}
          </div>
        </section>
      </main>
    </div>
  );
}
