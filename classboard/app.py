"""Classboard: a course announcement board.

Staff post room changes and deadline extensions. Students read them.
The process stays up when the database is down so Kubernetes can use
/health as a liveness probe and /ready as a readiness probe.
"""

from __future__ import annotations

import json
import os
import re
import signal
import threading
from datetime import UTC, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlparse

COURSE_RE = re.compile(r"[A-Z0-9]{2,16}")
MAX_BODY_BYTES = 8192
STATIC_INDEX = Path(__file__).with_name("static") / "index.html"

SCHEMA = """
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_code TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    author_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
"""


class MemoryStore:
    """In-memory stand-in used by tests. One process, no durability."""

    def __init__(self) -> None:
        self._rows: list[dict[str, Any]] = []
        self._next = 1
        self._lock = threading.Lock()
        self._up = True

    def ping(self) -> bool:
        return self._up

    def list_announcements(self, course: str | None) -> list[dict[str, Any]]:
        with self._lock:
            rows = list(self._rows)
        if course is not None:
            rows = [row for row in rows if row["course_code"] == course]
        rows.sort(key=lambda row: (row["created_at"], row["id"]), reverse=True)
        return rows

    def create_announcement(
        self, course: str, title: str, body: str, author: str
    ) -> dict[str, Any]:
        with self._lock:
            row = {
                "id": self._next,
                "course_code": course,
                "title": title,
                "body": body,
                "author_name": author,
                "created_at": datetime.now(UTC).isoformat(),
            }
            self._next += 1
            self._rows.append(row)
            return dict(row)


class PostgresStore:
    """One shared connection. The board is small; a pool would not change the design."""

    def __init__(self, **kwargs: Any) -> None:
        self._kwargs = kwargs
        self._conn: Any = None
        self._lock = threading.Lock()

    @classmethod
    def from_env(cls) -> PostgresStore:
        if not os.environ.get("DB_HOST"):
            raise RuntimeError("DB_HOST is required")
        return cls(
            host=os.environ["DB_HOST"],
            port=int(os.environ.get("DB_PORT", "5432")),
            dbname=os.environ.get("DB_NAME", "classboard"),
            user=os.environ.get("DB_USER", "classboard"),
            password=os.environ.get("DB_PASSWORD", ""),
        )

    def ping(self) -> bool:
        try:
            with self._lock:
                conn = self._connect()
                with conn.cursor() as cur:
                    cur.execute("SELECT 1")
                    cur.fetchone()
            return True
        except Exception as exc:
            print(f"ERROR db ping failed: {exc}", flush=True)
            self._drop()
            return False

    def list_announcements(self, course: str | None) -> list[dict[str, Any]]:
        with self._lock:
            conn = self._connect()
            with conn.cursor() as cur:
                if course is None:
                    cur.execute(
                        """
                        SELECT id, course_code, title, body, author_name, created_at
                        FROM announcements
                        ORDER BY created_at DESC, id DESC
                        """
                    )
                else:
                    cur.execute(
                        """
                        SELECT id, course_code, title, body, author_name, created_at
                        FROM announcements
                        WHERE course_code = %s
                        ORDER BY created_at DESC, id DESC
                        """,
                        (course,),
                    )
                return [_row_dict(row) for row in cur.fetchall()]

    def create_announcement(
        self, course: str, title: str, body: str, author: str
    ) -> dict[str, Any]:
        with self._lock:
            conn = self._connect()
            with conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO announcements (course_code, title, body, author_name)
                    VALUES (%s, %s, %s, %s)
                    RETURNING id, course_code, title, body, author_name, created_at
                    """,
                    (course, title, body, author),
                )
                return _row_dict(cur.fetchone())

    def _connect(self) -> Any:
        import psycopg

        if self._conn is None or self._conn.closed:
            self._conn = psycopg.connect(connect_timeout=3, **self._kwargs)
            self._conn.autocommit = True
            with self._conn.cursor() as cur:
                cur.execute(SCHEMA)
        return self._conn

    def _drop(self) -> None:
        conn = self._conn
        self._conn = None
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass


def _row_dict(row: tuple[Any, ...]) -> dict[str, Any]:
    created = row[5]
    if hasattr(created, "isoformat"):
        created = created.isoformat()
    return {
        "id": row[0],
        "course_code": row[1],
        "title": row[2],
        "body": row[3],
        "author_name": row[4],
        "created_at": created,
    }


def validate_announcement(payload: Any) -> tuple[dict[str, str] | None, str | None]:
    if not isinstance(payload, dict):
        return None, "Body must be a JSON object"
    fields = {}
    for key, limit in (
        ("course_code", 16),
        ("title", 120),
        ("body", 2000),
        ("author_name", 80),
    ):
        value = payload.get(key, "")
        if not isinstance(value, str):
            return None, f"{key} must be a string"
        fields[key] = value.strip()
        if not fields[key] or len(fields[key]) > limit:
            return None, f"{key} is required and must be at most {limit} characters"
    fields["course_code"] = fields["course_code"].upper()
    if COURSE_RE.fullmatch(fields["course_code"]) is None:
        return None, "course_code must be 2-16 letters or digits"
    return fields, None


def make_handler(store: Any, version: str) -> type[BaseHTTPRequestHandler]:
    index_html = STATIC_INDEX.read_text(encoding="utf-8")

    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def do_GET(self) -> None:  # noqa: N802
            path, query = _split(self.path)
            if path == "/health":
                self._json(200, {"status": "ok", "version": version})
            elif path == "/ready":
                if store.ping():
                    self._json(200, {"status": "ready"})
                else:
                    self._json(503, {"status": "not-ready"})
            elif path == "/api/announcements":
                self._list(query)
            elif path == "/":
                body = index_html.encode("utf-8")
                self._bytes(200, "text/html; charset=utf-8", body)
            else:
                self._json(404, {"error": "not found"})

        def do_POST(self) -> None:  # noqa: N802
            path, _query = _split(self.path)
            if path != "/api/announcements":
                self._json(404, {"error": "not found"})
                return
            raw, error = self._read_body()
            if error is not None:
                self._json(400, {"error": error})
                return
            try:
                payload = json.loads(raw)
            except json.JSONDecodeError:
                self._json(400, {"error": "Body must be JSON"})
                return
            fields, problem = validate_announcement(payload)
            if problem is not None or fields is None:
                self._json(400, {"error": problem})
                return
            try:
                if not store.ping():
                    self._json(503, {"error": "database unavailable"})
                    return
                created = store.create_announcement(
                    fields["course_code"],
                    fields["title"],
                    fields["body"],
                    fields["author_name"],
                )
            except Exception as exc:
                print(f"ERROR create failed: {exc}", flush=True)
                self._json(503, {"error": "database unavailable"})
                return
            self._json(201, created)

        def log_message(self, fmt: str, *args: Any) -> None:
            return

        def _list(self, query: dict[str, list[str]]) -> None:
            raw_course = query.get("course", [""])[0].strip().upper()
            course: str | None = raw_course or None
            if course is not None and COURSE_RE.fullmatch(course) is None:
                self._json(400, {"error": "course must be 2-16 letters or digits"})
                return
            try:
                rows = store.list_announcements(course)
            except Exception as exc:
                print(f"ERROR list failed: {exc}", flush=True)
                self._json(503, {"error": "database unavailable"})
                return
            self._json(200, {"announcements": rows})

        def _read_body(self) -> tuple[str | None, str | None]:
            length = self.headers.get("Content-Length")
            if length is None or not length.isdigit():
                return None, "Content-Length is required"
            size = int(length)
            if size > MAX_BODY_BYTES:
                return None, "Body is too large"
            return self.rfile.read(size).decode("utf-8"), None

        def _json(self, status: int, payload: dict[str, Any]) -> None:
            body = json.dumps(payload).encode("utf-8")
            self._bytes(status, "application/json; charset=utf-8", body)

        def _bytes(self, status: int, content_type: str, body: bytes) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Connection", "close")
            self.end_headers()
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError):
                return
            print(
                f"INFO method={self.command} path={self.path} status={status}",
                flush=True,
            )

    return Handler


def _split(raw_path: str) -> tuple[str, dict[str, list[str]]]:
    parsed = urlparse(raw_path)
    return parsed.path, parse_qs(parsed.query)


def serve(store: Any, version: str, host: str, port: int) -> None:
    server = ThreadingHTTPServer((host, port), make_handler(store, version))
    server.daemon_threads = True

    def stop(signum: int, _frame: Any) -> None:
        # Raising here is the whole handler. Calling server.shutdown() from
        # the signal would wait for this same thread to leave serve_forever,
        # and the process would sit until Kubernetes sent SIGKILL.
        print(f"INFO signal={signum} shutting down", flush=True)
        raise SystemExit(0)

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    print(
        f"INFO listening host={host} port={port} version={version}",
        flush=True,
    )
    server.serve_forever()


def main() -> None:
    version = os.environ.get("APP_VERSION", "1")
    port = int(os.environ.get("PORT", "8080"))
    serve(PostgresStore.from_env(), version, "0.0.0.0", port)


if __name__ == "__main__":
    main()
