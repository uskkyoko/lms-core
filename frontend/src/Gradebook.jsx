import { useEffect, useState } from "react";
import { changeGrade, fetchGradebook, fetchMyGrades } from "./api.js";
import { formatDate, formatStatus } from "./format.js";

export default function Gradebook({ activeUser, course, onNotice, onError }) {
  const isStudent = activeUser?.role === "STUDENT";
  const [gradebook, setGradebook] = useState(null);
  const [myGrades, setMyGrades] = useState(null);
  const [edit, setEdit] = useState(null);

  async function load() {
    if (isStudent) {
      setMyGrades(await fetchMyGrades(activeUser.id, course.id));
      setGradebook(null);
    } else {
      setGradebook(await fetchGradebook(activeUser.id, course.id));
      setMyGrades(null);
    }
  }

  useEffect(() => {
    if (!activeUser || !course) {
      return;
    }
    load().catch((err) => onError(err.message));
  }, [activeUser?.id, course?.id]);

  async function onChangeGrade(event) {
    event.preventDefault();
    try {
      await changeGrade(activeUser.id, edit.gradeId, {
        points: Number(edit.points),
        feedback: edit.feedback,
        reason: edit.reason,
      });
      setEdit(null);
      onNotice("Grade updated. The change was written to the grade history.");
      await load();
    } catch (err) {
      onError(err.message);
    }
  }

  if (isStudent) {
    const cells = myGrades?.student?.cells || [];
    return (
      <article className="panel">
        <h2>Grades</h2>
        <p className="muted">You can see only your own grades and instructor feedback.</p>
        {!myGrades ? (
          <p className="muted">Loading grades…</p>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Status</th>
                  <th>Score</th>
                  <th>Feedback</th>
                </tr>
              </thead>
              <tbody>
                {cells.map((cell) => {
                  const item = myGrades.items.find((entry) => entry.type === (cell.type || "QUIZ") && entry.id === (cell.assignmentId || cell.quizId));
                  return (
                    <tr key={`${cell.type || "QUIZ"}-${cell.assignmentId || cell.quizId}`}>
                      <td>{item?.title}</td>
                      <td>{formatStatus(cell.status)}</td>
                      <td>
                        {cell.status === "PENDING_GRADING"
                          ? "Pending grading"
                          : cell.status === "GRADED"
                            ? `${cell.points} / ${cell.maxPoints}`
                            : "—"}
                      </td>
                      <td>{cell.feedback || "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <p>
              <strong>
                Course total: {myGrades.student.totalPoints} / {myGrades.student.maxPoints} (
                {myGrades.student.percentage}%)
              </strong>
            </p>
          </>
        )}
      </article>
    );
  }

  return (
    <article className="panel">
      <h2>Gradebook</h2>
      <p className="muted">All enrolled students, quiz and assignment items, totals, percentages, and grade history.</p>
      {!gradebook ? (
        <p className="muted">Loading gradebook…</p>
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Student</th>
                  {gradebook.items.map((item) => (
                    <th key={`${item.type}-${item.id}`}>
                      {item.title}
                      <div className="muted">{item.maxPoints} pts</div>
                    </th>
                  ))}
                  <th>Total</th>
                  <th>%</th>
                </tr>
              </thead>
              <tbody>
                {gradebook.students.map((row) => (
                  <tr key={row.id}>
                    <td>{row.fullName}</td>
                    {row.cells.map((cell) => (
                      <td key={`${cell.type || "QUIZ"}-${cell.assignmentId || cell.quizId}`}>
                        {cell.status === "PENDING_GRADING"
                          ? "Pending grading"
                          : cell.status === "GRADED"
                            ? `${cell.points} / ${cell.maxPoints}`
                            : formatStatus(cell.status)}
                        {cell.gradeId ? (
                          <button
                            className="linkish"
                            type="button"
                            onClick={() =>
                              setEdit({
                                gradeId: cell.gradeId,
                                student: row.fullName,
                                title: gradebook.items.find((item) => item.type === (cell.type || "QUIZ") && item.id === (cell.assignmentId || cell.quizId))?.title,
                                points: cell.points,
                                maxPoints: cell.maxPoints,
                                feedback: cell.feedback || "",
                                reason: "",
                              })
                            }
                          >
                            Change
                          </button>
                        ) : null}
                      </td>
                    ))}
                    <td>
                      {row.totalPoints} / {row.maxPoints}
                    </td>
                    <td>{row.percentage}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {edit ? (
            <form className="panel nested" onSubmit={onChangeGrade}>
              <h3>
                Change grade · {edit.student} · {edit.title}
              </h3>
              <div className="inline-fields">
                <label>
                  Points (max {edit.maxPoints})
                  <input
                    type="number"
                    min="0"
                    max={edit.maxPoints}
                    value={edit.points}
                    onChange={(event) => setEdit({ ...edit, points: event.target.value })}
                    required
                  />
                </label>
                <label>
                  Feedback
                  <input
                    value={edit.feedback}
                    onChange={(event) => setEdit({ ...edit, feedback: event.target.value })}
                  />
                </label>
                <label>
                  Reason
                  <input
                    value={edit.reason}
                    onChange={(event) => setEdit({ ...edit, reason: event.target.value })}
                  />
                </label>
              </div>
              <div className="actions">
                <button className="primary" type="submit">
                  Save grade
                </button>
                <button type="button" onClick={() => setEdit(null)}>
                  Cancel
                </button>
              </div>
            </form>
          ) : null}
          <h3>Grade history</h3>
          {gradebook.history.length === 0 ? (
            <p className="muted">No grade changes yet. History cannot be edited or deleted.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Student</th>
                  <th>Item</th>
                  <th>Old</th>
                  <th>New</th>
                  <th>Changed by</th>
                  <th>Reason</th>
                </tr>
              </thead>
              <tbody>
                {gradebook.history.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDate(log.changedAt)}</td>
                    <td>{log.studentName}</td>
                    <td>{log.itemTitle}</td>
                    <td>{log.oldValue}</td>
                    <td>{log.newValue}</td>
                    <td>
                      {log.changedByName} ({String(log.changedByRole).toLowerCase()})
                    </td>
                    <td>{log.reason || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </article>
  );
}
