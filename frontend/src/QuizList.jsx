import { useEffect, useState } from "react";
import {
  createQuiz,
  fetchAttempt,
  fetchModules,
  fetchQuizAttempts,
  fetchQuizzes,
  gradeEssays,
  saveAnswer,
  startQuiz,
  submitAttempt,
} from "./api.js";
import { formatDate, formatStatus, formatTimer, toApiDateTime, toDateTimeLocal } from "./format.js";

function emptyQuestion() {
  return {
    text: "",
    type: "MULTIPLE_CHOICE",
    points: 10,
    options: [
      { text: "", correct: true },
      { text: "", correct: false },
    ],
  };
}

export default function QuizList({ activeUser, course, onNotice, onError }) {
  const [quizzes, setQuizzes] = useState([]);
  const [modules, setModules] = useState([]);
  const [view, setView] = useState("list");
  const [attempt, setAttempt] = useState(null);
  const [attempts, setAttempts] = useState([]);
  const [selectedQuiz, setSelectedQuiz] = useState(null);
  const [saving, setSaving] = useState("");
  const [quizForm, setQuizForm] = useState({
    title: "",
    description: "",
    moduleId: "",
    timeLimitMinutes: 10,
    openAt: toDateTimeLocal(),
    closeAt: toDateTimeLocal(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
    questions: [emptyQuestion()],
  });

  const isStudent = activeUser?.role === "STUDENT";
  const canManage = activeUser?.role === "INSTRUCTOR" || activeUser?.role === "ADMINISTRATOR";

  async function loadQuizzes() {
    const [nextQuizzes, nextModules] = await Promise.all([
      fetchQuizzes(activeUser.id, course.id),
      fetchModules(activeUser.id, course.id),
    ]);
    setQuizzes(nextQuizzes);
    setModules(nextModules);
    setQuizForm((current) =>
      current.moduleId || !nextModules.length ? current : { ...current, moduleId: String(nextModules[0].id) }
    );
  }

  useEffect(() => {
    if (!activeUser || !course) {
      return;
    }
    setView("list");
    setAttempt(null);
    loadQuizzes().catch((err) => onError(err.message));
  }, [activeUser?.id, course?.id]);

  useEffect(() => {
    if (!attempt || attempt.status !== "IN_PROGRESS") {
      return undefined;
    }
    const timer = setInterval(() => {
      setAttempt((current) => {
        if (!current || current.status !== "IN_PROGRESS") {
          return current;
        }
        const remainingSeconds = Math.max(0, current.remainingSeconds - 1);
        if (remainingSeconds === 0 && current.remainingSeconds > 0) {
          submitAttempt(activeUser.id, current.id)
            .then((data) => {
              setAttempt(data);
              setView("results");
              onNotice("Time limit expired: the quiz was submitted automatically.");
              loadQuizzes().catch(() => {});
            })
            .catch((err) => onError(err.message));
        }
        return { ...current, remainingSeconds };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [attempt?.id, attempt?.status, activeUser?.id]);

  async function onStart(quiz) {
    try {
      const data = await startQuiz(activeUser.id, quiz.id);
      setAttempt(data);
      setView("take");
    } catch (err) {
      onError(err.message);
    }
  }

  async function openAttempt(attemptId, nextView) {
    try {
      const data = await fetchAttempt(activeUser.id, attemptId);
      setAttempt(data);
      setView(nextView);
    } catch (err) {
      onError(err.message);
    }
  }

  async function onSave(question, extra) {
    if (!attempt || attempt.status !== "IN_PROGRESS") {
      return;
    }
    setSaving("Saving answers…");
    try {
      const data = await saveAnswer(activeUser.id, attempt.id, {
        questionId: question.id,
        optionId: extra.optionId ?? question.selectedOptionId,
        answerText: extra.answerText ?? question.answerText,
      });
      setAttempt(data);
      setSaving("Answers saved.");
    } catch (err) {
      onError(err.message);
    }
  }

  async function onSubmit() {
    try {
      for (const question of attempt.questions) {
        await saveAnswer(activeUser.id, attempt.id, {
          questionId: question.id,
          optionId: question.selectedOptionId,
          answerText: question.answerText,
        });
      }
      const data = await submitAttempt(activeUser.id, attempt.id);
      setAttempt(data);
      setView("results");
      onNotice(
        data.status === "PENDING_GRADING"
          ? "Quiz submitted. Result: Pending grading until essay questions are graded."
          : "Quiz submitted. Multiple-choice questions were graded automatically."
      );
      await loadQuizzes();
    } catch (err) {
      onError(err.message);
    }
  }

  async function onCreateQuiz(event) {
    event.preventDefault();
    try {
      await createQuiz(activeUser.id, course.id, {
        ...quizForm,
        moduleId: quizForm.moduleId ? Number(quizForm.moduleId) : null,
        timeLimitMinutes: Number(quizForm.timeLimitMinutes),
        openAt: toApiDateTime(quizForm.openAt),
        closeAt: toApiDateTime(quizForm.closeAt),
      });
      setView("list");
      onNotice("Quiz saved.");
      await loadQuizzes();
    } catch (err) {
      onError(err.message);
    }
  }

  async function openInstructorAttempts(quiz) {
    try {
      const data = await fetchQuizAttempts(activeUser.id, quiz.id);
      setSelectedQuiz(quiz);
      setAttempts(data);
      setView("attempts");
    } catch (err) {
      onError(err.message);
    }
  }

  async function onGradeEssays() {
    const payload = {
      answers: attempt.questions
        .filter((question) => question.type === "ESSAY")
        .map((question) => ({
          questionId: question.id,
          points: Number(question.pointsAwarded ?? 0),
          feedback: question.feedback || "",
        })),
    };
    try {
      const data = await gradeEssays(activeUser.id, attempt.id, payload);
      setAttempt(data);
      onNotice("Essay grades saved. The quiz result is now available to the student.");
      await loadQuizzes();
    } catch (err) {
      onError(err.message);
    }
  }

  if (view === "take" && attempt) {
    return (
      <article className="panel">
        <div className="toolbar">
          <h2>{attempt.quizTitle}</h2>
          <div className={`timer ${attempt.remainingSeconds < 60 ? "warn" : ""}`}>
            Time left {formatTimer(attempt.remainingSeconds)}
          </div>
        </div>
        <p className="muted">{saving || "Answers are saved automatically so a lost connection does not erase work."}</p>
        {attempt.questions.map((question, index) => (
          <section key={question.id} className="question">
            <h3>
              Question {index + 1} · {question.points} pts
            </h3>
            <p>{question.text}</p>
            {question.type === "MULTIPLE_CHOICE" ? (
              question.options.map((option) => (
                <label key={option.id} className="option">
                  <input
                    type="radio"
                    name={`q-${question.id}`}
                    checked={question.selectedOptionId === option.id}
                    onChange={() => onSave(question, { optionId: option.id })}
                  />
                  {option.text}
                </label>
              ))
            ) : (
              <textarea
                value={question.answerText || ""}
                onChange={(event) => {
                  const answerText = event.target.value;
                  setAttempt((current) => ({
                    ...current,
                    questions: current.questions.map((item) =>
                      item.id === question.id ? { ...item, answerText } : item
                    ),
                  }));
                }}
                onBlur={(event) => onSave(question, { answerText: event.target.value })}
                placeholder="Write your answer"
              />
            )}
          </section>
        ))}
        <button className="primary" type="button" onClick={onSubmit}>
          Submit quiz
        </button>
      </article>
    );
  }

  if ((view === "results" || view === "review" || view === "grade") && attempt) {
    const pending = attempt.status === "PENDING_GRADING";
    return (
      <article className="panel">
        <div className="toolbar">
          <h2>{view === "grade" ? "Grade essay questions" : "Quiz results"}</h2>
          <button type="button" onClick={() => setView("list")}>
            Back to quizzes
          </button>
        </div>
        <p>
          {pending
            ? "Pending grading"
            : `Score: ${attempt.score ?? "—"} / ${attempt.maxPoints}`}
        </p>
        <p className="muted">
          Submitted {formatDate(attempt.submittedAt)} · {attempt.student.fullName}
        </p>
        {attempt.questions.map((question, index) => (
          <section key={question.id} className="question">
            <h3>
              Question {index + 1} · {question.points} pts
            </h3>
            <p>{question.text}</p>
            {question.type === "MULTIPLE_CHOICE" ? (
              question.options.map((option) => (
                <div
                  key={option.id}
                  className={`option ${
                    attempt.revealAnswers && option.correct
                      ? "correct"
                      : question.selectedOptionId === option.id
                        ? "selected"
                        : ""
                  }`}
                >
                  {option.text}
                  {question.selectedOptionId === option.id ? " · selected" : ""}
                  {attempt.revealAnswers && option.correct ? " · correct" : ""}
                </div>
              ))
            ) : (
              <p className="essay">{question.answerText || "No essay answer submitted."}</p>
            )}
            {attempt.revealAnswers && question.pointsAwarded != null ? (
              <p className="muted">
                Points awarded: {question.pointsAwarded}. {question.feedback}
              </p>
            ) : null}
            {view === "grade" && question.type === "ESSAY" ? (
              <div className="inline-fields">
                <label>
                  Points
                  <input
                    type="number"
                    min="0"
                    max={question.points}
                    value={question.pointsAwarded ?? ""}
                    onChange={(event) =>
                      setAttempt((current) => ({
                        ...current,
                        questions: current.questions.map((item) =>
                          item.id === question.id ? { ...item, pointsAwarded: event.target.value } : item
                        ),
                      }))
                    }
                  />
                </label>
                <label>
                  Feedback
                  <input
                    value={question.feedback || ""}
                    onChange={(event) =>
                      setAttempt((current) => ({
                        ...current,
                        questions: current.questions.map((item) =>
                          item.id === question.id ? { ...item, feedback: event.target.value } : item
                        ),
                      }))
                    }
                  />
                </label>
              </div>
            ) : null}
          </section>
        ))}
        {view === "grade" ? (
          <button className="primary" type="button" onClick={onGradeEssays}>
            Save essay grades
          </button>
        ) : null}
        {view === "results" && attempt.revealAnswers ? (
          <button type="button" onClick={() => setView("review")}>
            Review answers
          </button>
        ) : null}
      </article>
    );
  }

  if (view === "attempts") {
    return (
      <article className="panel">
        <div className="toolbar">
          <h2>{selectedQuiz?.title} attempts</h2>
          <button type="button" onClick={() => setView("list")}>
            Back
          </button>
        </div>
        {attempts.length === 0 ? (
          <p className="muted">No student attempts yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>Status</th>
                <th>Score</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {attempts.map((item) => (
                <tr key={item.id}>
                  <td>{item.student.fullName}</td>
                  <td>{formatStatus(item.status)}</td>
                  <td>
                    {item.status === "PENDING_GRADING"
                      ? "Pending grading"
                      : `${item.score ?? "—"} / ${item.maxPoints}`}
                  </td>
                  <td>
                    <button
                      type="button"
                      onClick={() => openAttempt(item.id, item.status === "PENDING_GRADING" ? "grade" : "review")}
                    >
                      {item.status === "PENDING_GRADING" ? "Grade essays" : "Review"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </article>
    );
  }

  if (view === "create") {
    return (
      <form className="panel stack-form" aria-label="Create quiz" onSubmit={onCreateQuiz}>
        <div className="toolbar">
          <h2>Create quiz</h2>
          <button type="button" onClick={() => setView("list")}>
            Cancel
          </button>
        </div>
        <label>
          Title
          <input
            value={quizForm.title}
            onChange={(event) => setQuizForm({ ...quizForm, title: event.target.value })}
            required
          />
        </label>
        <label>
          Description
          <textarea
            value={quizForm.description}
            onChange={(event) => setQuizForm({ ...quizForm, description: event.target.value })}
          />
        </label>
        {modules.length ? (
          <label>
            Module
            <select
              value={quizForm.moduleId}
              onChange={(event) => setQuizForm({ ...quizForm, moduleId: event.target.value })}
              required
            >
              {modules.map((module) => (
                <option key={module.id} value={module.id}>
                  {module.title}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        <div className="inline-fields">
          <label>
            Time limit (minutes)
            <input
              type="number"
              min="1"
              value={quizForm.timeLimitMinutes}
              onChange={(event) => setQuizForm({ ...quizForm, timeLimitMinutes: event.target.value })}
              required
            />
          </label>
          <label>
            Opens
            <input
              type="datetime-local"
              value={quizForm.openAt}
              onChange={(event) => setQuizForm({ ...quizForm, openAt: event.target.value })}
              required
            />
          </label>
          <label>
            Closes
            <input
              type="datetime-local"
              value={quizForm.closeAt}
              onChange={(event) => setQuizForm({ ...quizForm, closeAt: event.target.value })}
              required
            />
          </label>
        </div>
        {quizForm.questions.map((question, index) => (
          <section key={index} className="question">
            <h3>Question {index + 1}</h3>
            <label>
              Question
              <input
                value={question.text}
                onChange={(event) => {
                  const questions = [...quizForm.questions];
                  questions[index] = { ...question, text: event.target.value };
                  setQuizForm({ ...quizForm, questions });
                }}
                required
              />
            </label>
            <div className="inline-fields">
              <label>
                Type
                <select
                  value={question.type}
                  onChange={(event) => {
                    const questions = [...quizForm.questions];
                    questions[index] = {
                      ...question,
                      type: event.target.value,
                      options: event.target.value === "MULTIPLE_CHOICE" ? emptyQuestion().options : [],
                    };
                    setQuizForm({ ...quizForm, questions });
                  }}
                >
                  <option value="MULTIPLE_CHOICE">Multiple choice</option>
                  <option value="ESSAY">Essay</option>
                </select>
              </label>
              <label>
                Points
                <input
                  type="number"
                  min="1"
                  value={question.points}
                  onChange={(event) => {
                    const questions = [...quizForm.questions];
                    questions[index] = { ...question, points: Number(event.target.value) };
                    setQuizForm({ ...quizForm, questions });
                  }}
                  required
                />
              </label>
            </div>
            {question.type === "MULTIPLE_CHOICE"
              ? question.options.map((option, optionIndex) => (
                  <label key={optionIndex} className="option">
                    <input
                      type="radio"
                      name={`correct-${index}`}
                      checked={option.correct}
                      onChange={() => {
                        const questions = [...quizForm.questions];
                        questions[index] = {
                          ...question,
                          options: question.options.map((item, current) => ({
                            ...item,
                            correct: current === optionIndex,
                          })),
                        };
                        setQuizForm({ ...quizForm, questions });
                      }}
                    />
                    <input
                      value={option.text}
                      placeholder={`Option ${optionIndex + 1}`}
                      onChange={(event) => {
                        const questions = [...quizForm.questions];
                        const options = [...question.options];
                        options[optionIndex] = { ...option, text: event.target.value };
                        questions[index] = { ...question, options };
                        setQuizForm({ ...quizForm, questions });
                      }}
                      required
                    />
                  </label>
                ))
              : null}
            {question.type === "MULTIPLE_CHOICE" ? (
              <button
                type="button"
                onClick={() => {
                  const questions = [...quizForm.questions];
                  questions[index] = {
                    ...question,
                    options: [...question.options, { text: "", correct: false }],
                  };
                  setQuizForm({ ...quizForm, questions });
                }}
              >
                Add option
              </button>
            ) : null}
          </section>
        ))}
        <button
          type="button"
          onClick={() => setQuizForm({ ...quizForm, questions: [...quizForm.questions, emptyQuestion()] })}
        >
          Add question
        </button>{" "}
        <button className="primary" type="submit">
          Save quiz
        </button>
      </form>
    );
  }

  return (
    <article className="panel">
      <div className="toolbar">
        <div>
          <h2>Quizzes</h2>
          <p className="muted">
            {isStudent
              ? "One attempt per quiz. A countdown timer submits automatically when time runs out."
              : "Create quizzes with multiple-choice and essay questions, then grade essays."}
          </p>
        </div>
        {canManage ? (
          <button className="primary" type="button" onClick={() => setView("create")}>
            Create quiz
          </button>
        ) : null}
      </div>
      {quizzes.length === 0 ? (
        <div className="empty">No quizzes in this course yet.</div>
      ) : (
        <div className="table-wrap">
        <table className="quiz-table">
          <thead>
            <tr>
              <th>Quiz</th>
              <th>Questions</th>
              <th>Points</th>
              <th>Time</th>
              <th>Window</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {quizzes.map((quiz) => (
              <tr key={quiz.id}>
                <td>
                  <strong>{quiz.title}</strong>
                  <div className="muted">{quiz.description}</div>
                </td>
                <td>{quiz.questionCount}</td>
                <td>{quiz.totalPoints}</td>
                <td>{quiz.timeLimitMinutes} min</td>
                <td className="nowrap">
                  {formatDate(quiz.openAt)} – {formatDate(quiz.closeAt)}
                </td>
                <td className="status-cell">
                  <span className={["CLOSED", "NOT_OPEN"].includes(isStudent ? quiz.studentStatus : quiz.availability) ? "muted" : undefined}>
                    {formatStatus(isStudent ? quiz.studentStatus : quiz.availability)}
                  </span>
                </td>
                <td className="quiz-actions">
                  {isStudent && quiz.studentStatus === "AVAILABLE" ? (
                    <button className="primary" type="button" onClick={() => onStart(quiz)}>
                      Start quiz
                    </button>
                  ) : null}
                  {isStudent && quiz.studentStatus === "IN_PROGRESS" ? (
                    <button className="primary" type="button" onClick={() => openAttempt(quiz.attemptId, "take")}>
                      Continue
                    </button>
                  ) : null}
                  {isStudent && quiz.attemptId && quiz.studentStatus !== "IN_PROGRESS" ? (
                    <button type="button" onClick={() => openAttempt(quiz.attemptId, "results")}>
                      {quiz.studentStatus === "GRADED" ? "Review" : "View result"}
                    </button>
                  ) : null}
                  {isStudent && !quiz.attemptId && quiz.studentStatus === "CLOSED" ? (
                    <span className="muted">Cannot start</span>
                  ) : null}
                  {isStudent && !quiz.attemptId && quiz.studentStatus === "NOT_OPEN" ? (
                    <span className="muted">Not open yet</span>
                  ) : null}
                  {canManage ? (
                    <button type="button" onClick={() => openInstructorAttempts(quiz)}>
                      Attempts
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      )}
    </article>
  );
}
