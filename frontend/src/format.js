export function formatDate(value) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString();
}

export function formatStatus(status) {
  const labels = {
    AVAILABLE: "Available",
    OPEN: "Open",
    NOT_OPEN: "Not open yet",
    CLOSED: "Closed",
    IN_PROGRESS: "In progress",
    PENDING_GRADING: "Pending grading",
    GRADED: "Graded",
    COMPLETED: "Graded",
    MISSING: "Missing",
  };
  return labels[status] || status || "—";
}

export function formatTimer(seconds) {
  const safe = Math.max(0, Number(seconds) || 0);
  const minutes = Math.floor(safe / 60);
  const rest = safe % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}

export function toDateTimeLocal(date = new Date()) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function toApiDateTime(value) {
  return value.length === 16 ? `${value}:00` : value;
}
