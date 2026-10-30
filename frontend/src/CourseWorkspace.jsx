import { useEffect, useState } from "react";
import {
  createAnnouncement,
  createAssignment,
  createDiscussion,
  createMaterial,
  createModule,
  deleteDiscussion,
  downloadNamedFile,
  editDiscussion,
  fetchAnnouncements,
  fetchAssignments,
  fetchDiscussions,
  fetchModules,
  fetchSubmissions,
  gradeSubmission,
  replyToDiscussion,
  setMaterialPublished,
  submitAssignment,
  uploadMaterial,
} from "./api.js";
import { formatDate, toApiDateTime, toDateTimeLocal } from "./format.js";

const TABS = [
  { id: "modules", label: "Modules" },
  { id: "assignments", label: "Assignments" },
  { id: "announcements", label: "Announcements" },
  { id: "discussions", label: "Discussions" },
];

const DEADLINE_MESSAGE = "The deadline has passed. This assignment no longer accepts new or replacement submissions.";

export default function CourseWorkspace({ course, activeUser, onError, onNotice }) {
  const [tab, setTab] = useState("modules");
  const [modules, setModules] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [discussions, setDiscussions] = useState([]);
  const [submissions, setSubmissions] = useState({});
  const user = activeUser;
  const isStaff = user?.role === "ADMINISTRATOR" || user?.role === "INSTRUCTOR";
  const isStudent = user?.role === "STUDENT";

  async function reload() {
    const [nextModules, nextAssignments, nextAnnouncements, nextDiscussions] = await Promise.all([
      fetchModules(user.id, course.id),
      fetchAssignments(user.id, course.id),
      fetchAnnouncements(user.id, course.id),
      fetchDiscussions(user.id, course.id),
    ]);
    setModules(nextModules);
    setAssignments(nextAssignments);
    setAnnouncements(nextAnnouncements);
    setDiscussions(nextDiscussions);
    const pairs = await Promise.all(
      nextAssignments.map(async (assignment) => [assignment.id, await fetchSubmissions(user.id, assignment.id)])
    );
    setSubmissions(Object.fromEntries(pairs));
  }

  useEffect(() => {
    reload().catch((err) => {
      if (err?.name !== "AbortError") onError(err.message);
    });
  }, [course?.id, user?.id]);

  async function run(action, successMessage) {
    onError("");
    onNotice("");
    try {
      await action();
      await reload();
      if (successMessage) onNotice(successMessage);
    } catch (err) {
      onError(err.message);
    }
  }

  if (!course || !user) return null;

  return (
    <section className="workspace">
      <div className="tabs">
        {TABS.map((item) => (
          <button key={item.id} type="button" className={tab === item.id ? "tab active" : "tab"} onClick={() => setTab(item.id)}>
            {item.label}
          </button>
        ))}
      </div>
      {tab === "modules" ? <ModulesTab modules={modules} isStaff={isStaff} user={user} courseId={course.id} run={run} /> : null}
      {tab === "assignments" ? (
        <AssignmentsTab assignments={assignments} submissions={submissions} isStaff={isStaff} isStudent={isStudent} user={user} courseId={course.id} modules={modules} run={run} />
      ) : null}
      {tab === "announcements" ? <AnnouncementsTab announcements={announcements} isStaff={isStaff} courseId={course.id} user={user} run={run} /> : null}
      {tab === "discussions" ? <DiscussionsTab discussions={discussions} isStaff={isStaff} user={user} courseId={course.id} run={run} /> : null}
    </section>
  );
}

function ModulesTab({ modules, isStaff, user, courseId, run }) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  return (
    <>
      {isStaff ? (
        <form className="panel stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => {
            await createModule(user.id, courseId, { title, description });
            setTitle("");
            setDescription("");
          }, "Module created.");
        }}>
          <h2>Add module</h2>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
          <label>Description<textarea value={description} onChange={(event) => setDescription(event.target.value)} /></label>
          <div className="actions"><button className="primary" type="submit">Save module</button></div>
        </form>
      ) : null}
      {modules.length === 0 ? <div className="empty">No modules in this course yet.</div> : null}
      {modules.map((module) => (
        <article className="panel" key={module.id}>
          <h2>{module.title}</h2>
          {module.description ? <p>{module.description}</p> : null}
          {module.materials.length === 0 ? <p className="muted">No published materials yet.</p> : null}
          <ul className="stack">
            {module.materials.map((material) => (
              <li key={material.id} className="item">
                <div>
                  <strong>{material.title}</strong>
                  <p className="muted">{material.type}{material.published ? "" : " · hidden"}{material.originalFileName ? ` · ${material.originalFileName}` : ""}</p>
                  {material.type === "TEXT" ? <p>{material.body}</p> : null}
                  {material.type === "VIDEO" && material.embedUrl ? (
                    <iframe className="video-frame" src={material.embedUrl} title={material.title} allowFullScreen />
                  ) : null}
                </div>
                <div className="actions">
                  {material.hasFile ? (
                    <button type="button" onClick={() => run(() => downloadNamedFile(`/api/materials/${material.id}/file`, user.id, material.originalFileName))}>Download</button>
                  ) : null}
                  {isStaff ? (
                    <button type="button" onClick={() => run(() => setMaterialPublished(user.id, material.id, !material.published), material.published ? "Material hidden." : "Material published.")}>
                      {material.published ? "Hide" : "Publish"}
                    </button>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
          {isStaff ? <AddMaterialForm user={user} moduleId={module.id} run={run} /> : null}
        </article>
      ))}
    </>
  );
}

function AddMaterialForm({ user, moduleId, run }) {
  const [title, setTitle] = useState("");
  const [type, setType] = useState("TEXT");
  const [body, setBody] = useState("");
  const [embedUrl, setEmbedUrl] = useState("");
  const [file, setFile] = useState(null);
  const [published, setPublished] = useState(true);
  return (
    <form className="nested stack-form" onSubmit={(event) => {
      event.preventDefault();
      run(async () => {
        if ((type === "FILE" || (type === "VIDEO" && file)) && file) {
          await uploadMaterial(user.id, moduleId, { title, type, published, file });
        } else {
          await createMaterial(user.id, moduleId, { title, type, body, embedUrl, published });
        }
        setTitle(""); setBody(""); setEmbedUrl(""); setFile(null);
      }, "Material saved.");
    }}>
      <h3>Add material</h3>
      <div className="form-row">
        <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
        <label>Type
          <select value={type} onChange={(event) => setType(event.target.value)}>
            <option value="TEXT">Text page</option>
            <option value="FILE">File (TXT, DOCX, PDF)</option>
            <option value="VIDEO">Video</option>
          </select>
        </label>
      </div>
      {type === "TEXT" ? <label>Content<textarea value={body} onChange={(event) => setBody(event.target.value)} required /></label> : null}
      {type === "VIDEO" ? <label>Embed link<input value={embedUrl} onChange={(event) => setEmbedUrl(event.target.value)} placeholder="https://www.youtube.com/embed/..." /></label> : null}
      {type === "FILE" || type === "VIDEO" ? (
        <label>File<input type="file" accept={type === "VIDEO" ? ".mp4,.webm" : ".txt,.docx,.pdf"} required={type === "FILE"} onChange={(event) => setFile(event.target.files[0] || null)} /></label>
      ) : null}
      <label className="inline"><input type="checkbox" checked={published} onChange={(event) => setPublished(event.target.checked)} /> Published</label>
      <div className="actions"><button className="primary" type="submit">Save material</button></div>
    </form>
  );
}

function AssignmentsTab({ assignments, submissions, isStaff, isStudent, user, courseId, modules, run }) {
  const [form, setForm] = useState({
    title: "",
    instructions: "",
    moduleId: "",
    deadline: toDateTimeLocal(new Date(Date.now() + 7 * 86400000)),
    maxPoints: 100,
    published: true,
  });
  return (
    <>
      {isStaff ? (
        <form className="panel stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => {
            await createAssignment(user.id, courseId, {
              title: form.title,
              instructions: form.instructions,
              moduleId: form.moduleId ? Number(form.moduleId) : modules[0]?.id,
              deadline: toApiDateTime(form.deadline),
              maxPoints: Number(form.maxPoints),
              published: form.published,
            });
            setForm({ ...form, title: "", instructions: "" });
          }, "Assignment created.");
        }}>
          <h2>Create assignment</h2>
          <label>Title<input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required /></label>
          <label>Instructions<textarea value={form.instructions} onChange={(event) => setForm({ ...form, instructions: event.target.value })} /></label>
          <div className="form-row">
            <label>Module
              <select value={form.moduleId} onChange={(event) => setForm({ ...form, moduleId: event.target.value })}>
                <option value="">First module</option>
                {modules.map((module) => <option key={module.id} value={module.id}>{module.title}</option>)}
              </select>
            </label>
            <label>Deadline<input type="datetime-local" value={form.deadline} onChange={(event) => setForm({ ...form, deadline: event.target.value })} required /></label>
            <label>Max points<input type="number" min="1" value={form.maxPoints} onChange={(event) => setForm({ ...form, maxPoints: event.target.value })} required /></label>
          </div>
          <label className="inline"><input type="checkbox" checked={form.published} onChange={(event) => setForm({ ...form, published: event.target.checked })} /> Published</label>
          <div className="actions"><button className="primary" type="submit">Save assignment</button></div>
        </form>
      ) : null}
      {assignments.length === 0 ? <div className="empty">No assignments yet.</div> : null}
      {assignments.map((assignment) => (
        <article className="panel" key={assignment.id}>
          <h2>{assignment.title}</h2>
          <p>{assignment.instructions}</p>
          <p className="muted">Due {formatDate(assignment.deadline)} · {assignment.maxPoints} points{assignment.published ? "" : " · hidden"}</p>
          {isStudent ? <StudentSubmit assignment={assignment} user={user} submission={(submissions[assignment.id] || [])[0]} run={run} /> : null}
          {isStaff ? <StaffSubmissions assignment={assignment} user={user} items={submissions[assignment.id] || []} run={run} /> : null}
        </article>
      ))}
    </>
  );
}

function StudentSubmit({ assignment, user, submission, run }) {
  const [file, setFile] = useState(null);
  const closed = new Date(assignment.deadline).getTime() <= Date.now();
  return (
    <form className="nested" onSubmit={(event) => {
      event.preventDefault();
      if (!closed) run(() => submitAssignment(user.id, assignment.id, file), "Submission saved.");
    }}>
      {submission ? (
        <p>Submitted {formatDate(submission.submittedAt)} as {submission.originalFileName}{submission.grade ? ` · ${submission.grade.points}/${submission.grade.maxPoints}. ${submission.grade.feedback || ""}` : ""}</p>
      ) : <p className="muted">No submission yet. TXT, DOCX, or PDF only.</p>}
      {submission ? <button type="button" onClick={() => run(() => downloadNamedFile(`/api/submissions/${submission.id}/file`, user.id, submission.originalFileName))}>Download my file</button> : null}
      {closed ? <p className="banner error">{DEADLINE_MESSAGE}</p> : (
        <>
          <label>File<input type="file" accept=".txt,.docx,.pdf" required onChange={(event) => setFile(event.target.files[0] || null)} /></label>
          <button className="primary" type="submit">{submission ? "Replace file" : "Submit"}</button>
        </>
      )}
    </form>
  );
}

function StaffSubmissions({ assignment, user, items, run }) {
  return (
    <div className="nested">
      <h3>Submissions</h3>
      {items.length === 0 ? <p className="muted">No submissions yet.</p> : null}
      {items.map((item) => <GradeRow key={item.id} assignment={assignment} user={user} item={item} run={run} />)}
    </div>
  );
}

function GradeRow({ assignment, user, item, run }) {
  const [points, setPoints] = useState(item.grade?.points ?? "");
  const [feedback, setFeedback] = useState(item.grade?.feedback ?? "");
  return (
    <div className="item">
      <div>
        <strong>{item.student.fullName}</strong>
        <p className="muted">{item.originalFileName} · {formatDate(item.submittedAt)}</p>
      </div>
      <div className="actions">
        <button type="button" onClick={() => run(() => downloadNamedFile(`/api/submissions/${item.id}/file`, user.id, item.originalFileName))}>Download</button>
      </div>
      <form className="grade-form" onSubmit={(event) => {
        event.preventDefault();
        run(() => gradeSubmission(user.id, item.id, { points: Number(points), feedback }), "Grade saved.");
      }}>
        <input type="number" min="0" max={assignment.maxPoints} step="0.5" value={points} onChange={(event) => setPoints(event.target.value)} required />
        <input value={feedback} onChange={(event) => setFeedback(event.target.value)} placeholder="Feedback" />
        <button className="primary" type="submit">Save grade</button>
      </form>
    </div>
  );
}

function AnnouncementsTab({ announcements, isStaff, courseId, user, run }) {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  return (
    <>
      {isStaff ? (
        <form className="panel stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => {
            await createAnnouncement(user.id, courseId, { title, body });
            setTitle(""); setBody("");
          }, "Announcement posted.");
        }}>
          <h2>Post announcement</h2>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
          <label>Message<textarea value={body} onChange={(event) => setBody(event.target.value)} required /></label>
          <div className="actions"><button className="primary" type="submit">Publish</button></div>
        </form>
      ) : null}
      {announcements.length === 0 ? <div className="empty">No announcements yet.</div> : null}
      {announcements.map((item) => (
        <article className="panel" key={item.id}>
          <h2>{item.title}</h2>
          <p className="muted">{item.author.fullName} · {formatDate(item.createdAt)}</p>
          <p>{item.body}</p>
        </article>
      ))}
    </>
  );
}

function DiscussionsTab({ discussions, isStaff, user, courseId, run }) {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  return (
    <>
      {isStaff ? (
        <form className="panel stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => {
            await createDiscussion(user.id, courseId, { title, body });
            setTitle(""); setBody("");
          }, "Discussion posted.");
        }}>
          <h2>Start a discussion</h2>
          <p className="muted">Students can reply to this thread. Only instructors and administrators can open a new discussion.</p>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
          <label>Post<textarea value={body} onChange={(event) => setBody(event.target.value)} required /></label>
          <div className="actions"><button className="primary" type="submit">Post</button></div>
        </form>
      ) : (
        <div className="empty">The instructor starts course discussions. Reply to a thread below.</div>
      )}
      {discussions.length === 0 ? <div className="empty">No discussion threads yet.</div> : null}
      {discussions.map((post) => <DiscussionThread key={post.id} post={post} user={user} isStaff={isStaff} run={run} />)}
    </>
  );
}

function ReplyItem({ item, user, isStaff, run }) {
  const [editing, setEditing] = useState(false);
  const [body, setBody] = useState(item.body);
  const canEdit = item.author.id === user.id;
  const canDelete = canEdit || isStaff;
  return (
    <>
      {editing ? (
        <form className="stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => { await editDiscussion(user.id, item.id, { body }); setEditing(false); }, "Reply updated.");
        }}>
          <label>Reply<textarea value={body} onChange={(event) => setBody(event.target.value)} required /></label>
          <div className="actions"><button className="primary" type="submit">Save</button></div>
        </form>
      ) : (
        <div>
          <strong>{item.author.fullName}</strong>
          <p className="muted">{formatDate(item.createdAt)}{item.updatedAt ? " · edited" : ""}</p>
          <p>{item.body}</p>
        </div>
      )}
      <div className="actions">
        {canEdit ? <button type="button" onClick={() => setEditing((current) => !current)}>{editing ? "Cancel" : "Edit"}</button> : null}
        {canDelete ? <button type="button" onClick={() => run(() => deleteDiscussion(user.id, item.id), "Reply deleted.")}>Delete</button> : null}
      </div>
    </>
  );
}

function DiscussionThread({ post, user, isStaff, run }) {
  const [reply, setReply] = useState("");
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState({ title: post.title || "", body: post.body });
  const canEdit = post.author.id === user.id;
  const canDelete = canEdit || isStaff;
  return (
    <article className="panel">
      {editing ? (
        <form className="stack-form" onSubmit={(event) => {
          event.preventDefault();
          run(async () => { await editDiscussion(user.id, post.id, draft); setEditing(false); }, "Post updated.");
        }}>
          <label>Title<input value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label>
          <label>Post<textarea value={draft.body} onChange={(event) => setDraft({ ...draft, body: event.target.value })} required /></label>
          <div className="actions"><button className="primary" type="submit">Save</button></div>
        </form>
      ) : (
        <>
          <h2>{post.title}</h2>
          <p className="muted">{post.author.fullName} · {formatDate(post.createdAt)}{post.updatedAt ? " · edited" : ""}</p>
          <p>{post.body}</p>
        </>
      )}
      <div className="actions">
        {canEdit ? <button type="button" onClick={() => setEditing((current) => !current)}>{editing ? "Cancel" : "Edit"}</button> : null}
        {canDelete ? <button type="button" onClick={() => run(() => deleteDiscussion(user.id, post.id), "Post deleted.")}>Delete</button> : null}
      </div>
      <ul className="stack">
        {(post.replies || []).map((item) => (
          <li key={item.id} className="item"><ReplyItem item={item} user={user} isStaff={isStaff} run={run} /></li>
        ))}
      </ul>
      <form className="nested stack-form" onSubmit={(event) => {
        event.preventDefault();
        run(async () => { await replyToDiscussion(user.id, post.id, reply); setReply(""); }, "Reply posted.");
      }}>
        <label>Reply<textarea value={reply} onChange={(event) => setReply(event.target.value)} required /></label>
        <div className="actions"><button className="primary" type="submit">Reply</button></div>
      </form>
    </article>
  );
}
