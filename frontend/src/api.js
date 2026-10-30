const headers = (userId, extra = {}) => ({
  ...(userId ? { "X-User-Id": String(userId) } : {}),
  ...extra,
});

async function readJson(response) {
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.message || "Request failed.");
  }
  return data;
}

export function fetchUsers(role) {
  const query = role ? `?role=${role}` : "";
  return fetch(`/api/users${query}`).then(readJson);
}

export function fetchCourses(userId, signal) {
  return fetch("/api/courses", { headers: headers(userId), signal }).then(readJson);
}

function courseRequest(userId, courseId, suffix, method, payload) {
  return fetch(`/api/courses/${courseId}${suffix}`, {
    method, headers: headers(userId, { "Content-Type": "application/json" }),
    body: payload === undefined ? undefined : JSON.stringify(payload),
  }).then(readJson);
}
export const updateCourse = (userId, courseId, payload) => courseRequest(userId, courseId, "", "PUT", payload);
export const archiveCourse = (userId, courseId) => courseRequest(userId, courseId, "/archive", "PATCH");
export const deleteCourse = (userId, courseId) => courseRequest(userId, courseId, "", "DELETE");
export const assignCourseInstructors = (userId, courseId, instructorIds) => courseRequest(userId, courseId, "/instructors", "PUT", { instructorIds });
export const removeStudent = (userId, courseId, studentId) => courseRequest(userId, courseId, `/enrollments/${studentId}`, "DELETE");
export const fetchProgress = (userId, courseId) => courseRequest(userId, courseId, "/progress", "GET");
export const fetchAnalytics = (userId, courseId) => courseRequest(userId, courseId, "/analytics", "GET");

export function createCourse(userId, payload) {
  return fetch("/api/courses", {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function enrollStudent(userId, courseId, studentId) {
  return fetch(`/api/courses/${courseId}/enrollments`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify({ studentId }),
  }).then(readJson);
}

export function fetchQuizzes(userId, courseId) {
  return fetch(`/api/courses/${courseId}/quizzes`, { headers: headers(userId) }).then(readJson);
}

export function createQuiz(userId, courseId, payload) {
  return fetch(`/api/courses/${courseId}/quizzes`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function startQuiz(userId, quizId) {
  return fetch(`/api/quizzes/${quizId}/start`, {
    method: "POST",
    headers: headers(userId),
  }).then(readJson);
}

export function fetchQuizAttempts(userId, quizId) {
  return fetch(`/api/quizzes/${quizId}/attempts`, { headers: headers(userId) }).then(readJson);
}

export function fetchAttempt(userId, attemptId) {
  return fetch(`/api/attempts/${attemptId}`, { headers: headers(userId) }).then(readJson);
}

export function saveAnswer(userId, attemptId, payload) {
  return fetch(`/api/attempts/${attemptId}/answer`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function submitAttempt(userId, attemptId) {
  return fetch(`/api/attempts/${attemptId}/submit`, {
    method: "POST",
    headers: headers(userId),
  }).then(readJson);
}

export function gradeEssays(userId, attemptId, payload) {
  return fetch(`/api/attempts/${attemptId}/essay-grades`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function fetchGradebook(userId, courseId) {
  return fetch(`/api/courses/${courseId}/gradebook`, { headers: headers(userId) }).then(readJson);
}

export function fetchMyGrades(userId, courseId) {
  return fetch(`/api/courses/${courseId}/grades`, { headers: headers(userId) }).then(readJson);
}

export function changeGrade(userId, gradeId, payload) {
  return fetch(`/api/grades/${gradeId}`, {
    method: "PUT",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}
