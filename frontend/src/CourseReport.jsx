import { useEffect, useState } from "react";
import { fetchProgress, fetchAnalytics } from "./api.js";

const labels = { MATERIAL: "Materials", ASSIGNMENT: "Assignments", QUIZ: "Quizzes" };
export default function CourseReport({ activeUser, course }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const student = activeUser.role === "STUDENT";
  async function load() {
    setLoading(true); setError("");
    try { setReport(await (student ? fetchProgress : fetchAnalytics)(activeUser.id, course.id)); }
    catch (failure) { setReport(null); setError(failure.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, [activeUser.id, course.id]);
  return <section className="panel" aria-label={student ? "Student progress" : "Instructor analytics"}>
    <div className="toolbar"><h2>{student ? "Course progress" : "Course analytics"}</h2><button onClick={load} disabled={loading}>Refresh report</button></div>
    {loading && <p role="status">Loading report...</p>}
    {error && <p role="alert" className="banner error">{error}</p>}
    {report && <>
      {report.unavailableSources.length > 0 && <p role="status" className="banner pending">{report.unavailableSources.map(k => labels[k]).join(" and ")} reporting will be available when those course features are added. The figures below cover available activities.</p>}
      {student ? <>
        <div className="progress-grid">{report.completion.map(item => <article key={item.kind}>
          <h3>{labels[item.kind]}</h3>
          {report.unavailableSources.includes(item.kind) ? <p>Not available yet</p> : <><strong>{item.completed} / {item.total}</strong><progress aria-label={`${labels[item.kind]} completion`} max={item.total || 1} value={item.completed} /><p className="muted">{item.percentage}% completed</p></>}
        </article>)}</div>
        <h3>Current grade</h3>
        <p>{report.currentGradePercentage === null ? "No assessment items yet" : `${report.currentGradePercentage}% (${report.earnedPoints} / ${report.possiblePoints} points)`}</p>
        <p className="muted">Matches the gradebook: ungraded and missing work earns no points in the current total.</p>
        {report.pendingGrades > 0 && <p>{report.pendingGrades} submitted assessment(s) pending grading.</p>}
      </> : <>
        <p><strong>{report.enrolledStudents}</strong> enrolled student(s)</p>
        <h3>Assessment performance</h3>
        {report.assessments.length ? <div className="table-wrap"><table><thead><tr><th>Assessment</th><th>Submitted</th><th>Rate</th><th>Average score</th></tr></thead>
          <tbody>{report.assessments.map(item => <tr key={`${item.kind}-${item.id}`}><td>{item.title}<small>{labels[item.kind]}</small></td><td>{item.submitted} / {report.enrolledStudents}</td><td>{item.submissionRate}%</td><td>{item.averageScore === null ? (item.submitted ? "Pending grading" : "No graded scores") : `${item.averageScore} / ${item.maximumPoints}`}</td></tr>)}</tbody></table></div> : <p>No assessment data yet.</p>}
        <h3>Students with missing work</h3>
        {report.missingWork.length ? <ul>{report.missingWork.map(item => <li key={`${item.studentId}-${item.kind}-${item.activityId}`}>{item.studentName} — {item.activityTitle}</li>)}</ul> : <p>No missing work among available assessments.</p>}
      </>}
    </>}
  </section>;
}
