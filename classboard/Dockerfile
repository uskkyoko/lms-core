# syntax=docker/dockerfile:1
# Final stage is python:3.12-slim, not alpine: psycopg's binary wheel is
# built for glibc. Compiling it on musl would put a toolchain back in the image.

FROM python:3.12-slim AS build
ENV PYTHONDONTWRITEBYTECODE=1
WORKDIR /build
RUN python -m venv /opt/venv
ENV PATH=/opt/venv/bin:$PATH
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && rm -rf /opt/venv/lib/python3.12/site-packages/pip* \
              /opt/venv/lib/python3.12/site-packages/setuptools* \
              /opt/venv/bin/pip*

FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH=/opt/venv/bin:$PATH \
    PORT=8080
RUN useradd --create-home --uid 10001 app
COPY --from=build /opt/venv /opt/venv
WORKDIR /app
COPY --chown=app:app app.py app.py
COPY --chown=app:app static static
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/health')"]
ENTRYPOINT ["python", "app.py"]
