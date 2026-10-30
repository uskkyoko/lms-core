import { useEffect, useState } from "react";
import { createCourse, updateCourse, assignCourseInstructors, enrollStudent, removeStudent } from "./api.js";

const blank = { title: "", code: "", description: "", term: "Fall 2026", instructorIds: [] };

export function CourseEditor({ actorId, course, users, onSaved, onCancel, onBusy }) {
  const [form, setForm] = useState(course ? { ...course, instructorIds: course.instructors.map(i => i.id) } : blank);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const change = event => setForm({ ...form, [event.target.name]: event.target.value });
  async function submit(event) {
    event.preventDefault(); setError("");
    if (!form.instructorIds.length) { setError("Assign at least one instructor."); return; }
    setBusy(true); onBusy(true);
    try {
      const payload = { title: form.title.trim(), code: form.code.trim(), description: form.description.trim(), term: form.term.trim(), instructorIds: form.instructorIds };
      const saved = course ? await updateCourse(actorId, course.id, payload) : await createCourse(actorId, payload);
      onSaved(saved);
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); onBusy(false); }
  }
  return <form className="panel" onSubmit={submit} aria-label={course ? "Edit course" : "Create course"}>
    <h2>{course ? "Edit course" : "Create course"}</h2>
    <fieldset disabled={busy} className="form-fields">
      <label>Title<input name="title" required maxLength={150} value={form.title} onChange={change} /></label>
      <label>Course code<input name="code" required maxLength={30} pattern="[A-Za-z0-9 -]+" value={form.code} onChange={change} /></label>
      <label>Term<input name="term" required maxLength={60} value={form.term} onChange={change} /></label>
      <label>Description<textarea name="description" required maxLength={2000} value={form.description} onChange={change} /></label>
      <fieldset className="instructor-picker"><legend>Assigned instructors</legend>
        {users.filter(u => u.role === "INSTRUCTOR").map(user => <label className="option" key={user.id}>
          <input type="checkbox" checked={form.instructorIds.includes(user.id)} onChange={event => setForm({ ...form, instructorIds: event.target.checked ? [...form.instructorIds, user.id] : form.instructorIds.filter(id => id !== user.id) })} />{user.fullName}
        </label>)}
      </fieldset>
    </fieldset>
    {error && <p role="alert" className="banner error">{error}</p>}
    <div className="actions"><button className="primary" disabled={busy}>Save course</button><button type="button" disabled={busy} onClick={onCancel}>Cancel</button></div>
  </form>;
}

export function CoursePeople({ actorId, course, users, onUpdated, onBusy }) {
  const [instructors, setInstructors] = useState(course.instructors.map(i => i.id));
  const [studentId, setStudentId] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => { setInstructors(course.instructors.map(i => i.id)); }, [course]);
  async function mutate(action, message) {
    setBusy(true); onBusy(true); setError(""); setNotice("");
    try { onUpdated(await action()); setNotice(message); }
    catch (failure) { setError(failure.message); }
    finally { setBusy(false); onBusy(false); }
  }
  return <section className="panel" aria-label="Manage people">
    <h2>Manage people</h2>
    {error && <p role="alert" className="banner error">{error}</p>}
    {notice && <p role="status" className="banner success">{notice}</p>}
    <form onSubmit={event => { event.preventDefault(); mutate(() => assignCourseInstructors(actorId, course.id, instructors), "Instructors updated."); }}>
      <fieldset disabled={busy}><legend>Assigned instructors</legend>
        {users.filter(u => u.role === "INSTRUCTOR").map(user => <label className="option" key={user.id}>
          <input type="checkbox" checked={instructors.includes(user.id)} onChange={event => setInstructors(event.target.checked ? [...instructors, user.id] : instructors.filter(id => id !== user.id))} />{user.fullName}
        </label>)}
        <button disabled={!instructors.length}>Save instructors</button>
      </fieldset>
    </form>
    <h3>Enrolled students</h3>
    <ul className="roster">{course.students.map(student => <li key={student.id}>
      <span>{student.fullName}</span><button disabled={busy} className="danger" aria-label={`Remove ${student.fullName}`} onClick={() => mutate(() => removeStudent(actorId, course.id, student.id), "Student removed. Course access revoked.")}>Remove</button>
    </li>)}</ul>
    {!course.students.length && <p className="muted">No students enrolled.</p>}
    <form className="form-fields" onSubmit={event => { event.preventDefault(); mutate(() => enrollStudent(actorId, course.id, Number(studentId)), "Student enrolled."); }}>
      <label>Student<select required disabled={busy} value={studentId} onChange={event => setStudentId(event.target.value)}>
        <option value="">Select a student</option>
        {users.filter(u => u.role === "STUDENT").map(user => <option key={user.id} value={user.id}>{user.fullName}{course.students.some(s => s.id === user.id) ? " (already enrolled)" : ""}</option>)}
      </select></label>
      <button className="primary" disabled={busy || !studentId}>Enroll student</button>
    </form>
  </section>;
}
