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

export function fetchModules(userId, courseId) {
  return fetch(`/api/courses/${courseId}/modules`, { headers: headers(userId) }).then(readJson);
}

export function createModule(userId, courseId, payload) {
  return fetch(`/api/courses/${courseId}/modules`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function createMaterial(userId, moduleId, payload) {
  return fetch(`/api/modules/${moduleId}/materials`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function uploadMaterial(userId, moduleId, { title, type, published, file }) {
  const body = new FormData();
  body.append("file", file);
  const query = new URLSearchParams({ title, type, published: String(!!published) });
  return fetch(`/api/modules/${moduleId}/materials/upload?${query}`, {
    method: "POST",
    headers: headers(userId),
    body,
  }).then(readJson);
}

export function setMaterialPublished(userId, materialId, published) {
  return fetch(`/api/materials/${materialId}/publish`, {
    method: "PATCH",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify({ published }),
  }).then(readJson);
}

export function fetchAssignments(userId, courseId) {
  return fetch(`/api/courses/${courseId}/assignments`, { headers: headers(userId) }).then(readJson);
}

export function createAssignment(userId, courseId, payload) {
  return fetch(`/api/courses/${courseId}/assignments`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function fetchSubmissions(userId, assignmentId) {
  return fetch(`/api/assignments/${assignmentId}/submissions`, { headers: headers(userId) }).then(readJson);
}

export function submitAssignment(userId, assignmentId, file) {
  const body = new FormData();
  body.append("file", file);
  return fetch(`/api/assignments/${assignmentId}/submissions`, {
    method: "POST",
    headers: headers(userId),
    body,
  }).then(readJson);
}

export function gradeSubmission(userId, submissionId, payload) {
  return fetch(`/api/submissions/${submissionId}/grade`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function fetchAnnouncements(userId, courseId) {
  return fetch(`/api/courses/${courseId}/announcements`, { headers: headers(userId) }).then(readJson);
}

export function createAnnouncement(userId, courseId, payload) {
  return fetch(`/api/courses/${courseId}/announcements`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function fetchDiscussions(userId, courseId) {
  return fetch(`/api/courses/${courseId}/discussions`, { headers: headers(userId) }).then(readJson);
}

export function createDiscussion(userId, courseId, payload) {
  return fetch(`/api/courses/${courseId}/discussions`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function replyToDiscussion(userId, postId, body) {
  return fetch(`/api/discussions/${postId}/replies`, {
    method: "POST",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify({ body }),
  }).then(readJson);
}

export function editDiscussion(userId, postId, payload) {
  return fetch(`/api/discussions/${postId}`, {
    method: "PUT",
    headers: headers(userId, { "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  }).then(readJson);
}

export function deleteDiscussion(userId, postId) {
  return fetch(`/api/discussions/${postId}`, {
    method: "DELETE",
    headers: headers(userId),
  }).then(async (response) => {
    if (response.status === 204 || response.ok && response.status !== 200) {
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || "Request failed.");
      }
      return;
    }
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || "Request failed.");
    }
  });
}

export async function downloadNamedFile(url, userId, fallbackName) {
  const response = await fetch(url, { headers: headers(userId) });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || "Download failed.");
  }
  const blob = await response.blob();
  const header = response.headers.get("content-disposition") || "";
  const match = header.match(/filename="([^"]+)"/);
  const filename = match?.[1] || fallbackName || "download";
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
