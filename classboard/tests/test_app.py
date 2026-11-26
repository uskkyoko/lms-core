import json
import os
import threading
from http.server import ThreadingHTTPServer

import pytest

from app import MemoryStore, PostgresStore, make_handler


@pytest.fixture()
def board():
    store = MemoryStore()
    server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(store, "test"))
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    base = f"http://127.0.0.1:{server.server_address[1]}"
    try:
        yield base, store
    finally:
        server.shutdown()


def _get(url):
    import urllib.request

    with urllib.request.urlopen(url, timeout=2) as response:
        return response.status, json.loads(response.read().decode())


def _post(url, payload):
    import urllib.error
    import urllib.request

    data = json.dumps(payload).encode()
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=2) as response:
            return response.status, json.loads(response.read().decode())
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read().decode())


def test_health_stays_up_without_reading_the_database(board):
    base, store = board
    store._up = False
    status, body = _get(base + "/health")
    assert status == 200
    assert body["status"] == "ok"
    assert body["version"] == "test"


def test_ready_reports_database(board):
    base, store = board
    status, body = _get(base + "/ready")
    assert status == 200
    assert body["status"] == "ready"
    store._up = False
    import urllib.error

    with pytest.raises(urllib.error.HTTPError) as caught:
        _get(base + "/ready")
    assert caught.value.code == 503


def test_post_and_list_round_trip(board):
    base, _store = board
    status, created = _post(
        base + "/api/announcements",
        {
            "course_code": "se331",
            "title": "Room change",
            "body": "Final moves to B204.",
            "author_name": "Andrii Shevchenko",
        },
    )
    assert status == 201
    assert created["course_code"] == "SE331"
    status, body = _get(base + "/api/announcements?course=SE331")
    assert status == 200
    assert body["announcements"][0]["title"] == "Room change"


def test_course_filter_excludes_other_courses(board):
    base, _store = board
    _post(
        base + "/api/announcements",
        {
            "course_code": "AI101",
            "title": "Lab cancelled",
            "body": "No lab today.",
            "author_name": "Olha Kovalenko",
        },
    )
    status, body = _get(base + "/api/announcements?course=SE331")
    assert status == 200
    assert body["announcements"] == []


def test_blank_title_is_rejected(board):
    base, _store = board
    status, body = _post(
        base + "/api/announcements",
        {
            "course_code": "SE331",
            "title": "   ",
            "body": "Something",
            "author_name": "Andrii",
        },
    )
    assert status == 400
    assert "title" in body["error"]


def test_pipeline_records_a_failure():
    # Removed in the following commit. Exists so the Actions run goes red
    # and the test report is still uploaded.
    assert False


def test_home_page_is_served(board):
    import urllib.request

    base, _store = board
    with urllib.request.urlopen(base + "/", timeout=2) as response:
        html = response.read().decode()
    assert response.status == 200
    assert "Classboard" in html


def test_postgres_round_trip():
    if not os.environ.get("DB_HOST"):
        pytest.skip("DB_HOST is not set")
    store = PostgresStore.from_env()
    assert store.ping()
    created = store.create_announcement(
        "CI101",
        "Pipeline notice",
        "Posted by the integration test.",
        "CI",
    )
    rows = store.list_announcements("CI101")
    assert any(row["id"] == created["id"] for row in rows)
