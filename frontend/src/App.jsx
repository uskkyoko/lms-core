import { useEffect, useMemo, useState } from "react";
import { archiveCourse, deleteCourse, fetchCourses, fetchUsers } from "./api.js";
import { CourseEditor, CoursePeople } from "./AdminCourse.jsx";
import CourseReport from "./CourseReport.jsx";
import Gradebook from "./Gradebook.jsx";
import QuizList from "./QuizList.jsx";

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
  const [section, setSection] = useState("dashboard");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [editor, setEditor] = useState(null);
  const activeUser = users.find(user => String(user.id) === String(activeUserId));
  const roleUsers = useMemo(() => users.filter(user => user.role === (activeUser?.role || "STUDENT")), [users, activeUser]);
  const selectedCourse = courses.find(course => course.id === selectedCourseId) || courses[0];
  const isAdmin = activeUser?.role === "ADMINISTRATOR";
  const canGrade = activeUser?.role === "INSTRUCTOR" || isAdmin;

  useEffect(() => {
    fetchUsers().then(data => {
      setUsers(data);
      if (!data.some(user => String(user.id) === String(activeUserId))) {
        const first = data.find(user => user.role === "STUDENT") || data[0];
        setActiveUserId(first ? String(first.id) : "");
      }
      if (!data.length) setLoading(false);
    }).catch(failure => { setError(failure.message); setLoading(false); });
  }, []);

  useEffect(() => {
    if (!activeUser) return;
    const controller = new AbortController();
    localStorage.setItem(STORAGE_KEY, String(activeUserId));
    setLoading(true); setCourses([]); setError("");
    fetchCourses(activeUserId, controller.signal).then(data => {
      if (controller.signal.aborted) return;
      setCourses(data);
      setSelectedCourseId(current => data.some(course => course.id === current) ? current : data[0]?.id || null);
    }).catch(failure => { if (!controller.signal.aborted) setError(failure.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [activeUserId, activeUser?.id]);

  function selectUser(id) {
    if (String(id) === String(activeUserId)) return;
    setCourses([]); setLoading(true); setSection("dashboard"); setEditor(null);
    setError(""); setNotice(""); setActiveUserId(String(id));
  }
  function updated(course) {
    setCourses(current => current.map(item => item.id === course.id ? course : item).sort((a,b) => a.code.localeCompare(b.code)));
  }
  function saved(course) {
    setCourses(current => [...current.filter(item => item.id !== course.id), course].sort((a,b) => a.code.localeCompare(b.code)));
    setSelectedCourseId(course.id); setEditor(null); setNotice("Course saved.");
  }
  async function act(kind) {
    const message = kind === "delete"
      ? `Delete ${selectedCourse.code} and its enrollments? Courses with assessments must be archived to preserve learning records.`
      : `Archive ${selectedCourse.code}? Its learning records will remain available.`;
    if (!window.confirm(message)) return;
    setBusy(true); setError(""); setNotice("");
    try {
      if (kind === "delete") {
        await deleteCourse(activeUserId, selectedCourse.id);
        setCourses(current => current.filter(course => course.id !== selectedCourse.id));
        setSelectedCourseId(null); setEditor(null); setNotice("Course deleted.");
      } else { updated(await archiveCourse(activeUserId, selectedCourse.id)); setNotice("Course archived."); }
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  const onError = message => { setError(message); setNotice(""); };
  const onNotice = message => { setNotice(message); setError(""); };
  const componentKey = `${activeUser?.id}-${selectedCourse?.id}`;

  return <div className="app">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark">AUK</span><div><strong>University LMS</strong><p>Teaching &amp; learning core</p></div></div>
      <nav aria-label="Workspace">
        {[['dashboard', 'Dashboard'], ['courses', isAdmin ? 'Course management' : 'My Courses'], ['quizzes', 'Quizzes'], ['grades', canGrade ? 'Gradebook' : 'Grades'],
          ...(activeUser?.role === 'STUDENT' ? [['progress','Progress']] : []), ...(activeUser?.role === 'INSTRUCTOR' ? [['analytics','Analytics']] : [])]
          .map(([value,label]) => <button className={`nav-item ${section === value ? 'active' : ''}`} type="button" disabled={busy || loading} key={value} onClick={() => { setSection(value); setEditor(null); }}>{label}</button>)}
      </nav>
    </aside>
    <main>
      <header className="topbar">
        <div><h1>{isAdmin ? "Administrator" : activeUser?.role === "INSTRUCTOR" ? "Instructor" : "Student"} Dashboard</h1>
          <p className="muted">{activeUser ? `${activeUser.fullName} · ${activeUser.role.toLowerCase()}` : 'Select a sample user'}</p></div>
        <div className="role-switcher">
          <label>Role<select value={activeUser?.role || 'STUDENT'} disabled={busy || loading} onChange={event => {
            const next = users.find(user => user.role === event.target.value); if (next) selectUser(next.id);
          }}>{ROLES.map(role => <option key={role.value} value={role.value}>{role.label}</option>)}</select></label>
          <label>Sample user<select value={activeUserId} disabled={busy || loading} onChange={event => selectUser(event.target.value)}>{roleUsers.map(user => <option key={user.id} value={user.id}>{user.fullName}</option>)}</select></label>
        </div>
      </header>
      {error && <div role="alert" className="banner error">{error}</div>}
      {notice && <div role="status" className="banner success">{notice}</div>}
      {loading && <p role="status">Loading courses...</p>}
      {activeUser && !loading && <>
        <div className="toolbar">
          <label>Selected course<select value={selectedCourse?.id || ''} disabled={busy} onChange={event => { setSelectedCourseId(Number(event.target.value)); setEditor(null); setNotice(''); }}>
            {!courses.length && <option value="">No courses available</option>}
            {courses.map(course => <option key={course.id} value={course.id}>{course.code} — {course.title}{course.status === 'ARCHIVED' ? ' (Archived)' : ''}</option>)}
          </select></label>
          {isAdmin && <button className="primary" disabled={busy} onClick={() => { setEditor({type:'create'}); setSection('courses'); }}>Create course</button>}
        </div>
        {!courses.length && <p className="empty">No courses for this user yet.</p>}
        {selectedCourse && section === 'quizzes' && <QuizList key={componentKey} activeUser={activeUser} course={selectedCourse} onError={onError} onNotice={onNotice} />}
        {selectedCourse && section === 'grades' && <Gradebook key={componentKey} activeUser={activeUser} course={selectedCourse} onError={onError} onNotice={onNotice} />}
        {selectedCourse && ['progress','analytics'].includes(section) && <CourseReport key={componentKey} activeUser={activeUser} course={selectedCourse} />}
        {['dashboard','courses'].includes(section) && <section className="grid">
          <div><h2>Available courses</h2><p className="muted">{isAdmin ? 'All courses in the system.' : activeUser.role === 'INSTRUCTOR' ? 'Courses assigned to you.' : 'Only courses you are enrolled in.'}</p>
            <div className="cards">{courses.map(course => <button key={course.id} type="button" disabled={busy} className={`card ${selectedCourse?.id === course.id ? 'selected' : ''}`} onClick={() => { setSelectedCourseId(course.id); setEditor(null); setSection('courses'); }}>
              <span className="code">{course.code} · {course.status === 'ARCHIVED' ? 'Archived' : 'Active'}</span><strong>{course.title}</strong><span className="muted">{course.term}</span>
            </button>)}</div>
          </div>
          <div>
            {editor && isAdmin ? <CourseEditor key={editor.type + (editor.course?.id || '')} actorId={activeUser.id} course={editor.course} users={users} onSaved={saved} onCancel={() => setEditor(null)} onBusy={setBusy} /> : selectedCourse && <>
              <article className="panel"><h2>{selectedCourse.code} — {selectedCourse.title}</h2><p>{selectedCourse.description}</p>
                <dl><div><dt>Term</dt><dd>{selectedCourse.term}</dd></div><div><dt>Status</dt><dd>{selectedCourse.status}</dd></div><div><dt>Instructors</dt><dd>{selectedCourse.instructors.map(user => user.fullName).join(', ') || 'Not assigned'}</dd></div>
                  {canGrade && <div><dt>Enrolled students</dt><dd>{selectedCourse.students.map(user => user.fullName).join(', ') || 'None'}</dd></div>}</dl>
                <div className="actions"><button className="primary" disabled={busy} onClick={() => setSection('quizzes')}>Quizzes</button><button disabled={busy} onClick={() => setSection('grades')}>{canGrade ? 'Gradebook' : 'Grades'}</button>
                  {!isAdmin && <button disabled={busy} onClick={() => setSection(activeUser.role === 'STUDENT' ? 'progress' : 'analytics')}>{activeUser.role === 'STUDENT' ? 'View progress' : 'View analytics'}</button>}
                  {isAdmin && <><button disabled={busy} onClick={() => setEditor({type:'edit',course:selectedCourse})}>Edit course</button><button disabled={busy || selectedCourse.status === 'ARCHIVED'} onClick={() => act('archive')}>Archive course</button><button className="danger" disabled={busy} onClick={() => act('delete')}>Delete course</button></>}
                </div>
              </article>
              {isAdmin && <CoursePeople key={componentKey} actorId={activeUser.id} course={selectedCourse} users={users} onUpdated={updated} onBusy={setBusy} />}
            </>}
          </div>
        </section>}
      </>}
    </main>
  </div>;
}
