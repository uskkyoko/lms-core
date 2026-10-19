import { useEffect, useRef, useState } from "react";
import { api } from "./api";

const roles = {
  ADMINISTRATOR: "Administrator",
  INSTRUCTOR: "Instructor",
  STUDENT: "Student",
};
const blankCourse = {
  title: "",
  code: "",
  description: "",
  term: "Fall 2026",
  instructorIds: [],
};

function BookIcon() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 5C8 3 5 3 2 4v15c3-1 6-1 10 1 4-2 7-2 10-1V4c-3-1-6-1-10 1Zm0 0v15"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function Modal({ title, onClose, children, busy }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
      aria-labelledby="modal-title"
    >
      <div className="modal-header">
        <h2 id="modal-title">{title}</h2>
        <button
          type="button"
          className="icon-button"
          aria-label="Close dialog"
          onClick={onClose}
          disabled={busy}
        >
          ×
        </button>
      </div>
      {children}
    </dialog>
  );
}

function CourseForm({ course, instructors, onClose, onSaved }) {
  const [values, setValues] = useState(
    course
      ? { ...course, instructorIds: course.instructors.map((i) => i.id) }
      : blankCourse,
  );
  const [error, setError] = useState("");
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);
  function change(event) {
    setValues({ ...values, [event.target.name]: event.target.value });
  }
  async function submit(event) {
    event.preventDefault();
    if (!values.instructorIds.length) {
      setError("Select at least one instructor.");
      return;
    }
    setBusy(true);
    setError("");
    setFields({});
    try {
      await api(course ? `/courses/${course.id}` : "/courses", {
        method: course ? "PUT" : "POST",
        body: {
          title: values.title.trim(),
          code: values.code.trim(),
          description: values.description.trim(),
          term: values.term.trim(),
          instructorIds: values.instructorIds,
        },
      });
      onSaved(course ? "Course updated." : "Course created successfully.");
    } catch (failure) {
      setError(failure.message);
      setFields(failure.fields || {});
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={course ? "Edit course" : "Create course"}
      onClose={onClose}
      busy={busy}
    >
      <p className="muted">
        Add the course details and choose its instructors. All fields are
        required.
      </p>
      <form onSubmit={submit}>
        <fieldset disabled={busy} className="form-fields">
          <label>
            Course title
            <input
              name="title"
              autoFocus
              required
              maxLength={150}
              value={values.title}
              onChange={change}
              placeholder="e.g. Introduction to Software Engineering"
            />
            {fields.title && (
              <small className="field-error">{fields.title}</small>
            )}
          </label>
          <div className="form-row">
            <label>
              Course code
              <input
                name="code"
                required
                maxLength={30}
                pattern="[A-Za-z0-9][A-Za-z0-9 \-]*"
                value={values.code}
                onChange={change}
                placeholder="e.g. SDT213"
              />
              {fields.code && (
                <small className="field-error">{fields.code}</small>
              )}
            </label>
            <label>
              Term
              <input
                name="term"
                required
                maxLength={60}
                value={values.term}
                onChange={change}
                placeholder="e.g. Fall 2026"
              />
              {fields.term && (
                <small className="field-error">{fields.term}</small>
              )}
            </label>
          </div>
          <label>
            Description
            <textarea
              name="description"
              required
              rows={4}
              maxLength={2000}
              value={values.description}
              onChange={change}
              placeholder="What will students learn in this course?"
            />
            {fields.description && (
              <small className="field-error">{fields.description}</small>
            )}
          </label>
          <fieldset className="instructor-picker">
            <legend>Assigned instructors</legend>
            {instructors.map((instructor) => (
              <label key={instructor.id} className="check-label">
                <input
                  type="checkbox"
                  checked={values.instructorIds.includes(instructor.id)}
                  onChange={(event) =>
                    setValues({
                      ...values,
                      instructorIds: event.target.checked
                        ? [...values.instructorIds, instructor.id]
                        : values.instructorIds.filter(
                            (id) => id !== instructor.id,
                          ),
                    })
                  }
                />
                {instructor.name}
              </label>
            ))}
          </fieldset>
        </fieldset>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <div className="modal-actions">
          <button
            type="button"
            className="secondary"
            onClick={onClose}
            disabled={busy}
          >
            Cancel
          </button>
          <button className="primary" disabled={busy}>
            {busy ? "Saving…" : course ? "Save changes" : "Create course"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function App() {
  const [users, setUsers] = useState([]);
  const [active, setActive] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);
  const admin = active?.role === "ADMINISTRATOR";

  async function loadCourses() {
    setLoading(true);
    setError("");
    try {
      setCourses(await api("/courses"));
    } catch (failure) {
      setCourses([]);
      setError(failure.message);
    } finally {
      setLoading(false);
    }
  }
  async function initialize() {
    setLoading(true);
    setError("");
    try {
      const sampleUsers = await api("/users");
      setUsers(sampleUsers);
      let current;
      try {
        current = await api("/session");
      } catch (failure) {
        if (failure.status !== 401) throw failure;
      }
      if (current) {
        setActive(current);
        setCourses(await api("/courses"));
      }
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    initialize();
  }, []);

  async function switchUser(event) {
    const selectedUser = event.target.value;
    setSwitching(true);
    setError("");
    setNotice("");
    setCourses([]);
    try {
      const user = await api("/session", {
        method: "PUT",
        body: { userId: Number(selectedUser) },
      });
      setActive(user);
      setQuery("");
      setFilter("ALL");
      setNotice(`Now viewing as ${user.name} · ${roles[user.role]}.`);
      await loadCourses();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSwitching(false);
    }
  }
  async function saved(message) {
    setModal(null);
    setNotice(message);
    await loadCourses();
  }
  async function confirmAction() {
    setBusy(true);
    setError("");
    try {
      await api(
        `/courses/${modal.course.id}${modal.type === "archive" ? "/archive" : ""}`,
        { method: modal.type === "archive" ? "PATCH" : "DELETE" },
      );
      await saved(
        modal.type === "archive" ? "Course archived." : "Course deleted.",
      );
    } catch (failure) {
      setError(failure.message);
      setModal(null);
    } finally {
      setBusy(false);
    }
  }
  const visible = courses.filter(
    (course) =>
      (filter === "ALL" || course.status === filter) &&
      `${course.title} ${course.code} ${course.term}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="University LMS home">
          <span className="brand-icon">
            <BookIcon />
          </span>
          <span>
            University<span className="brand-sub">LMS CORE</span>
          </span>
        </a>
        <p className="nav-caption">WORKSPACE</p>
        <a className="nav-item selected" href="#courses">
          <BookIcon />
          {admin ? "Course management" : "My courses"}
        </a>
        <div className="sidebar-footer">
          American University Kyiv
          <br />
          <span>Team 1 · Fall 2026</span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="header-role-chooser">
            <span className="avatar" aria-hidden="true">
              {active ? active.name[0] : "U"}
            </span>
            <label htmlFor="active-user">
              View as
              <select
                id="active-user"
                aria-label="Role"
                value={active?.id || ""}
                onChange={switchUser}
                disabled={loading || switching}
              >
                <option value="" disabled>
                  Select a role
                </option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {roles[user.role]} - {user.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </header>
        <main id="courses">
          <div className="heading-row">
            <div>
              <h1>{admin ? "Course management" : "My courses"}</h1>
              <p className="muted">
                {admin
                  ? "A home for every course. Create and manage your university’s curriculum."
                  : active?.role === "INSTRUCTOR"
                    ? "The courses you teach, together in one place."
                    : "Your enrolled courses, together in one place."}
              </p>
            </div>
            {admin && (
              <button
                className="primary"
                disabled={loading || switching}
                onClick={() => {
                  setNotice("");
                  setModal({ type: "create" });
                }}
              >
                <span aria-hidden="true">＋</span> Create course
              </button>
            )}
          </div>
          {notice && (
            <div role="status" className="success">
              ✓ {notice}
            </div>
          )}
          {error && (
            <div role="alert" className="error">
              {error}{" "}
              <button
                className="text-button"
                onClick={active ? loadCourses : initialize}
              >
                Try again
              </button>
            </div>
          )}
          {!active && !loading && !error && (
            <section className="empty">
              <BookIcon />
              <h2>Welcome to your university workspace</h2>
              <p>Choose a role in the top-right header to get started.</p>
            </section>
          )}
          {active && (
            <>
              <section className="summary-grid" aria-label="Course overview">
                <div>
                  <span>{admin ? "Total courses" : "Your courses"}</span>
                  <strong>{courses.length.toString().padStart(2, "0")}</strong>
                </div>
                <div>
                  <span>Active courses</span>
                  <strong>
                    {courses
                      .filter((course) => course.status === "ACTIVE")
                      .length.toString()
                      .padStart(2, "0")}
                  </strong>
                </div>
                <div>
                  <span>Archived courses</span>
                  <strong>
                    {courses
                      .filter((course) => course.status === "ARCHIVED")
                      .length.toString()
                      .padStart(2, "0")}
                  </strong>
                </div>
              </section>
              <div className="list-toolbar">
                <h2>
                  Course directory{" "}
                  <span className="count">{visible.length}</span>
                </h2>
                <div>
                  <label className="search">
                    <span className="sr-only">Search courses</span>
                    <input
                      type="search"
                      placeholder="Search title, code or term…"
                      value={query}
                      onChange={(event) => setQuery(event.target.value)}
                    />
                  </label>
                  <label>
                    <span className="sr-only">Course status</span>
                    <select
                      value={filter}
                      onChange={(event) => setFilter(event.target.value)}
                    >
                      <option value="ALL">All statuses</option>
                      <option value="ACTIVE">Active</option>
                      <option value="ARCHIVED">Archived</option>
                    </select>
                  </label>
                </div>
              </div>
            </>
          )}
          {loading && (
            <p role="status" className="loading">
              Loading your workspace…
            </p>
          )}
          {active && !loading && !error && (
            <div className="course-grid">
              {visible.map((course) => (
                <article className="course-card" key={course.id}>
                  <div className="card-top">
                    <span className="course-code">{course.code}</span>
                    <span className={`badge ${course.status.toLowerCase()}`}>
                      {course.status === "ACTIVE" ? "Active" : "Archived"}
                    </span>
                  </div>
                  <h3>{course.title}</h3>
                  <p className="course-description">{course.description}</p>
                  <dl>
                    <div>
                      <dt>TERM</dt>
                      <dd>{course.term}</dd>
                    </div>
                    <div>
                      <dt>
                        {course.instructors.length > 1
                          ? "INSTRUCTORS"
                          : "INSTRUCTOR"}
                      </dt>
                      <dd>
                        {course.instructors
                          .map((instructor) => instructor.name)
                          .join(", ")}
                      </dd>
                    </div>
                  </dl>
                  {admin && (
                    <div className="card-actions">
                      <button
                        className="text-button"
                        onClick={() => setModal({ type: "edit", course })}
                      >
                        Edit course <span aria-hidden="true">↗</span>
                      </button>
                      <div>
                        <button
                          className="quiet-button"
                          disabled={course.status === "ARCHIVED"}
                          onClick={() => setModal({ type: "archive", course })}
                        >
                          Archive
                        </button>
                        <button
                          className="quiet-button danger"
                          onClick={() => setModal({ type: "delete", course })}
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
          {active && !loading && !error && visible.length === 0 && (
            <section className="empty">
              <BookIcon />
              <h2>
                {courses.length ? "No matching courses" : "No courses yet"}
              </h2>
              <p>
                {courses.length
                  ? "Try another search or status filter."
                  : admin
                    ? "Create a course to get started."
                    : "Courses will appear here when you are assigned or enrolled."}
              </p>
            </section>
          )}
          <footer className="page-footer">
            University LMS{" "}
            <span>Built for teaching. Designed for learning.</span>
          </footer>
        </main>
      </div>
      {(modal?.type === "create" || modal?.type === "edit") && (
        <CourseForm
          course={modal.course}
          instructors={users.filter((user) => user.role === "INSTRUCTOR")}
          onClose={() => setModal(null)}
          onSaved={saved}
        />
      )}
      {(modal?.type === "archive" || modal?.type === "delete") && (
        <Modal
          title={
            modal.type === "archive" ? "Archive course?" : "Delete course?"
          }
          onClose={() => setModal(null)}
          busy={busy}
        >
          <p>
            {modal.type === "archive" ? (
              <>
                Mark <strong>{modal.course.title}</strong> as archived? Its
                information will remain available.
              </>
            ) : (
              <>
                Permanently delete <strong>{modal.course.title}</strong> and its
                sample enrollments? This cannot be undone.
              </>
            )}
          </p>
          <div className="modal-actions">
            <button
              className="secondary"
              disabled={busy}
              onClick={() => setModal(null)}
            >
              Cancel
            </button>
            <button
              className={modal.type === "delete" ? "destructive" : "primary"}
              disabled={busy}
              onClick={confirmAction}
            >
              {busy
                ? "Saving…"
                : modal.type === "delete"
                  ? "Delete course"
                  : "Archive course"}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
