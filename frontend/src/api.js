const headers = (userId, extra = {}) => ({
  ...(userId ? { "X-User-Id": String(userId) } : {}),
  ...extra,
});

async function readJson(response) {
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

export function fetchCourses(userId) {
  return fetch("/api/courses", { headers: headers(userId) }).then(readJson);
}

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
