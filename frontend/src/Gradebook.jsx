import { useEffect, useState } from "react";
import {
  changeGrade,
  downloadNamedFile,
  fetchAttempt,
  fetchGradebook,
  fetchMyGrades,
  fetchSubmissions,
  gradeEssays,
  gradeSubmission,
} from "./api.js";
import { formatDate, formatStatus } from "./format.js";

function itemForCell(items, cell) {
  const type = cell.type || "QUIZ";
  const id = cell.assignmentId || cell.quizId;
  return items.find((entry) => entry.type === type && entry.id === id);
}

function cellKey(cell) {
  return `${cell.type || "QUIZ"}-${cell.assignmentId || cell.quizId}`;
}

function hasWork(cell) {
  return Boolean(cell.attemptId) || Boolean(cell.submissionId);
}

export default function Gradebook({ activeUser, course, onNotice, onError }) {
  const isStudent = activeUser?.role === "STUDENT";
  const canMutate = activeUser?.role === "INSTRUCTOR";
  const [gradebook, setGradebook] = useState(null);
  const [myGrades, setMyGrades] = useState(null);
  const [edit, setEdit] = useState(null);
  const [review, setReview] = useState(null);

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

  async function openWork(row, cell, item) {
    setEdit(null);
    setReview({
      loading: true,
      student: row.fullName,
      title: item?.title,
      type: cell.type,
      cell,
    });
    try {
      if (cell.type === "QUIZ" && cell.attemptId) {
        const attempt = await fetchAttempt(activeUser.id, cell.attemptId);
        setReview({
          loading: false,
          student: row.fullName,
          title: item?.title,
          type: "QUIZ",
          cell,
          attempt,
        });
        return;
      }
      if (cell.submissionId) {
        const submissions = await fetchSubmissions(activeUser.id, cell.assignmentId);
        const submission = submissions.find((entry) => entry.id === cell.submissionId) || submissions[0];
        setReview({
          loading: false,
          student: row.fullName,
          title: item?.title,
          type: "ASSIGNMENT",
          cell,
          submission,
          points: submission?.grade?.points ?? "",
          feedback: submission?.grade?.feedback || "",
        });
        return;
      }
      setReview(null);
    } catch (err) {
      setReview(null);
      onError(err.message);
    }
  }

  async function onGradeEssays() {
    const payload = {
      answers: review.attempt.questions
        .filter((question) => question.type === "ESSAY")
        .map((question) => ({
          questionId: question.id,
          points: Number(question.pointsAwarded ?? 0),
          feedback: question.feedback || "",
        })),
    };
    try {
      const attempt = await gradeEssays(activeUser.id, review.attempt.id, payload);
      setReview({ ...review, attempt });
      onNotice("Essay grades saved. The quiz result is now available to the student.");
      await load();
    } catch (err) {
      onError(err.message);
    }
  }

  async function onGradeSubmission(event) {
    event.preventDefault();
    try {
      const submission = await gradeSubmission(activeUser.id, review.submission.id, {
        points: Number(review.points),
        feedback: review.feedback,
      });
      setReview({ ...review, submission, points: submission.grade?.points ?? review.points, feedback: submission.grade?.feedback || review.feedback });
      onNotice("Grade saved.");
      await load();
    } catch (err) {
      onError(err.message);
    }
  }

  if (isStudent) {
    const cells = myGrades?.student?.cells || [];
    return (
      <article className="panel" aria-label="My grades">
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
                  const item = itemForCell(myGrades.items, cell);
                  return (
                    <tr key={cellKey(cell)}>
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
    <article className="panel" aria-label="Gradebook">
      <h2>Gradebook</h2>
      <p className="muted">Open a submitted quiz or assignment to see student answers, then grade from here.</p>
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
                    {row.cells.map((cell) => {
                      const item = itemForCell(gradebook.items, cell);
                      return (
                        <td key={cellKey(cell)}>
                          {cell.status === "PENDING_GRADING"
                            ? "Pending grading"
                            : cell.status === "GRADED"
                              ? `${cell.points} / ${cell.maxPoints}`
                              : formatStatus(cell.status)}
                          {hasWork(cell) ? (
                            <div className="grade-cell-actions">
                              <button
                                className="linkish"
                                type="button"
                                onClick={() => openWork(row, cell, item)}
                              >
                                {cell.type === "ASSIGNMENT" ? "View work" : "View answers"}
                              </button>
                              {canMutate && cell.gradeId ? (
                                <button
                                  className="linkish"
                                  type="button"
                                  onClick={() =>
                                    setEdit({
                                      gradeId: cell.gradeId,
                                      student: row.fullName,
                                      title: item?.title,
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
                            </div>
                          ) : canMutate && cell.gradeId ? (
                            <button
                              className="linkish"
                              type="button"
                              onClick={() =>
                                setEdit({
                                  gradeId: cell.gradeId,
                                  student: row.fullName,
                                  title: item?.title,
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
                      );
                    })}
                    <td>
                      {row.totalPoints} / {row.maxPoints}
                    </td>
                    <td>{row.percentage}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {review ? (
            <WorkReview
              review={review}
              setReview={setReview}
              userId={activeUser.id}
              onError={onError}
              canMutate={canMutate}
              onGradeEssays={onGradeEssays}
              onGradeSubmission={onGradeSubmission}
            />
          ) : null}
          {canMutate && edit ? (
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

function WorkReview({ review, setReview, userId, onError, canMutate, onGradeEssays, onGradeSubmission }) {
  const pendingQuiz = review.attempt?.status === "PENDING_GRADING";
  return (
    <section className="panel nested" aria-label="Student work">
      <div className="toolbar">
        <h3>
          {review.student} · {review.title}
        </h3>
        <button type="button" onClick={() => setReview(null)}>
          Close
        </button>
      </div>
      {review.loading ? <p className="muted">Loading student work…</p> : null}
      {review.attempt ? (
        <>
          <p>
            {pendingQuiz
              ? "Pending grading — score the essay answers below."
              : `Score: ${review.attempt.score ?? "—"} / ${review.attempt.maxPoints}`}
          </p>
          <p className="muted">
            Submitted {formatDate(review.attempt.submittedAt)} · {review.attempt.student.fullName}
          </p>
          {review.attempt.questions.map((question, index) => (
            <section key={question.id} className="question">
              <h4>
                Question {index + 1} · {question.points} pts
              </h4>
              <p>{question.text}</p>
              {question.type === "MULTIPLE_CHOICE" ? (
                question.options.map((option) => (
                  <div
                    key={option.id}
                    className={`option ${
                      option.correct ? "correct" : question.selectedOptionId === option.id ? "selected" : ""
                    }`}
                  >
                    {option.text}
                    {question.selectedOptionId === option.id ? " · student answer" : ""}
                    {option.correct ? " · correct" : ""}
                  </div>
                ))
              ) : (
                <p className="essay">{question.answerText || "No essay answer submitted."}</p>
              )}
              {question.pointsAwarded != null && !pendingQuiz ? (
                <p className="muted">
                  Points awarded: {question.pointsAwarded}. {question.feedback}
                </p>
              ) : null}
              {canMutate && pendingQuiz && question.type === "ESSAY" ? (
                <div className="inline-fields">
                  <label>
                    Points
                    <input
                      type="number"
                      min="0"
                      max={question.points}
                      value={question.pointsAwarded ?? ""}
                      onChange={(event) =>
                        setReview({
                          ...review,
                          attempt: {
                            ...review.attempt,
                            questions: review.attempt.questions.map((item) =>
                              item.id === question.id ? { ...item, pointsAwarded: event.target.value } : item
                            ),
                          },
                        })
                      }
                    />
                  </label>
                  <label>
                    Feedback
                    <input
                      value={question.feedback || ""}
                      onChange={(event) =>
                        setReview({
                          ...review,
                          attempt: {
                            ...review.attempt,
                            questions: review.attempt.questions.map((item) =>
                              item.id === question.id ? { ...item, feedback: event.target.value } : item
                            ),
                          },
                        })
                      }
                    />
                  </label>
                </div>
              ) : null}
            </section>
          ))}
          {canMutate && pendingQuiz ? (
            <button className="primary" type="button" onClick={onGradeEssays}>
              Save essay grades
            </button>
          ) : null}
        </>
      ) : null}
      {review.submission ? (
        <>
          <p>
            Submitted {formatDate(review.submission.submittedAt)} as {review.submission.originalFileName}
          </p>
          <div className="actions">
            <button
              type="button"
              onClick={() =>
                downloadNamedFile(
                  `/api/submissions/${review.submission.id}/file`,
                  userId,
                  review.submission.originalFileName
                ).catch((err) => onError(err.message))
              }
            >
              Download submission
            </button>
          </div>
          {canMutate ? <form className="grade-form" onSubmit={onGradeSubmission}>
            <input
              type="number"
              min="0"
              max={review.cell.maxPoints}
              step="0.5"
              value={review.points}
              onChange={(event) => setReview({ ...review, points: event.target.value })}
              required
            />
            <input
              value={review.feedback}
              onChange={(event) => setReview({ ...review, feedback: event.target.value })}
              placeholder="Feedback"
            />
            <button className="primary" type="submit">
              Save grade
            </button>
          </form> : null}
        </>
      ) : null}
    </section>
  );
}
