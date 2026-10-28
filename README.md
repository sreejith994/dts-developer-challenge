# Caseworker Task Tracker

A small full-stack application that lets HMCTS caseworkers create, view, update and delete their tasks.
It was built for the DTS developer technical test.

- **Backend** – Spring Boot REST API backed by PostgreSQL, with validation, consistent error responses and
  OpenAPI documentation.
- **Frontend** – React single-page app with a paged task table, task details, inline status changes and a
  create-task form.

## Contents

- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration (`.env`)](#configuration-env)
- [Running the project](#running-the-project)
- [Building](#building)
- [Testing](#testing)
- [API reference](#api-reference)
- [Error handling](#error-handling)
- [Troubleshooting](#troubleshooting)

## Tech stack

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, Bean Validation |
| Database | PostgreSQL 17, schema managed by Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Frontend | React 19, TypeScript, Vite, lucide-react icons, plain CSS |
| Backend tests | JUnit 5, Mockito, MockMvc, RestTestClient, Testcontainers |
| Frontend tests | Vitest, React Testing Library, jsdom |

## Project structure

```
.
├── backend/                      Spring Boot API
│   ├── docker-compose.yml        Local PostgreSQL container
│   ├── .env                      Local configuration (not committed – see below)
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../tracker/
│       │   ├── controller/       REST endpoints
│       │   ├── service/          Business logic
│       │   ├── repository/       Spring Data JPA repository
│       │   ├── domain/           JPA entity and status enum
│       │   ├── dto/              Request/response records
│       │   ├── exception/        Error handling (RFC 9457 problem details)
│       │   └── config/           OpenAPI configuration
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/migration/     Flyway migrations
│       └── test/                 Unit, controller and integration tests
└── frontend/                     React app
    ├── package.json
    ├── vite.config.ts            Dev server + /api proxy to the backend
    └── src/
        ├── api/                  API client and types
        ├── components/           Table, modals, status controls
        └── styles.css
```

## Prerequisites

| Tool | Version | Used for |
| --- | --- | --- |
| Java JDK | 21 or newer | Building and running the backend |
| Docker (with Docker Compose) | Recent | Local PostgreSQL and the backend integration tests (Testcontainers) |
| Node.js | 20 or newer (npm included) | Building and running the frontend |

Maven does not need to be installed – the backend includes the Maven wrapper (`./mvnw`).

## Configuration (`.env`)

The backend reads its database settings from a `.env` file in the **`backend/`** directory. The same file is used
by Docker Compose to create the local PostgreSQL container, so both always agree.

The file contains credentials, so it is git-ignored and **must be created before the first run**:

```bash
cd backend
cat > .env <<'EOF'
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tracker
DB_USERNAME=tracker
DB_PASSWORD=change-me
EOF
```

| Variable | Description |
| --- | --- |
| `DB_HOST` | Database host the API connects to (`localhost` for the Compose container) |
| `DB_PORT` | Host port PostgreSQL is exposed on |
| `DB_NAME` | Database name (created by Compose on first start) |
| `DB_USERNAME` | Database user (created by Compose on first start) |
| `DB_PASSWORD` | Database password – choose your own value |

Notes:

- `application.properties` has no defaults for these values, so the backend will not start without them.
- The values can also be supplied as real environment variables (e.g. in CI or a container platform), which take
  precedence over the file.
- The `.env` file is loaded relative to the working directory, so run the backend from `backend/`.
- PostgreSQL only applies `DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` when its data volume is first created. If you
  change them later, recreate the volume (see [Troubleshooting](#troubleshooting)).
- The backend tests do **not** need `.env`; they start their own throwaway database with Testcontainers.

## Running the project

### 1. Start the database

```bash
cd backend
docker compose up -d
```

This starts PostgreSQL 17 in a container named `tracker-postgres`, with data kept in the `postgres-data` volume.
Check it is healthy with `docker compose ps`.

### 2. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

On startup Flyway creates or updates the schema automatically. The API is then available at
http://localhost:8080:

| URL | Description |
| --- | --- |
| http://localhost:8080/api/tasks | Task API |
| http://localhost:8080/swagger-ui.html | Interactive API documentation |
| http://localhost:8080/v3/api-docs | OpenAPI specification (JSON) |
| http://localhost:8080/actuator/health | Health check (includes database status) |

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` requests to the backend on port 8080. To use a backend on another address, set `API_URL`, e.g.
`API_URL=http://localhost:9090 npm run dev`.

### Stopping

- Frontend / backend: `Ctrl+C` in their terminals.
- Database: `docker compose down` in `backend/` (data is kept). Use `docker compose down -v` to also delete the data.

## Building

### Backend

```bash
cd backend
./mvnw clean package
```

This compiles the code, runs all tests and produces `target/tracker-0.0.1-SNAPSHOT.jar`, which can be run with:

```bash
cd backend
java -jar target/tracker-0.0.1-SNAPSHOT.jar
```

Add `-DskipTests` to build without running the tests (Docker is then not required).

### Frontend

```bash
cd frontend
npm run build
```

This type-checks the code and writes a production build to `frontend/dist/`. Preview it locally with
`npm run preview` (the `/api` proxy also applies there). In a deployed environment the static files need to be
served from the same origin as the API, or behind a reverse proxy that routes `/api` to the backend.

## Testing

### Backend

Docker must be running – the integration tests start a PostgreSQL container via Testcontainers.

```bash
cd backend
./mvnw test
```

| Test class | Type | What it covers |
| --- | --- | --- |
| `TaskServiceTest` | Unit (Mockito) | Business logic: creation, defaults, paging/sorting, not-found handling |
| `TaskControllerTest` | Web layer (MockMvc) | Request mapping, validation errors, status codes, paging parameters |
| `TaskApiIntegrationTest` | Integration (real HTTP, random port, PostgreSQL container) | Full create/read/update/delete lifecycle, paging, error responses |
| `TrackerApplicationTests` | Integration | Testcontainers database is up and used, Flyway migrations applied |

Run a single test class with `./mvnw test -Dtest=TaskApiIntegrationTest`.

### Frontend

```bash
cd frontend
npm test            # run once
npm run test:watch  # watch mode
```

The tests cover the API client (including error parsing), the create-task form validation and submission, the paged
table (pagination, inline status updates, empty and error states), the task details dialog and the date helpers.

## API reference

Base path: `/api/tasks`. Full, interactive documentation is available in Swagger UI when the backend is running.

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/tasks` | Create a task | `201` `{ "id": 1 }` |
| `GET` | `/api/tasks?page=0&size=20` | Page of tasks, ordered by due date (earliest first) | `200` paged response |
| `GET` | `/api/tasks/all` | Every task, unpaged, ordered by due date | `200` array of tasks |
| `GET` | `/api/tasks/{id}` | Get a task by id | `200` task |
| `PATCH` | `/api/tasks/{id}/status` | Update a task's status | `200` updated task |
| `DELETE` | `/api/tasks/{id}` | Delete a task | `204` |

### Task model

| Field | Type | Notes |
| --- | --- | --- |
| `id` | number | Auto-incrementing, assigned by the database |
| `title` | string | Required, max 255 characters, surrounding whitespace trimmed |
| `description` | string \| null | Optional, max 2000 characters |
| `status` | string | `CREATED`, `INPROGRESS`, `BLOCKED`, `COMPLETED` or `CANCELLED`; new tasks always start as `CREATED` |
| `dueDate` | ISO-8601 date-time with offset | Required, must be in the future when creating, e.g. `2026-12-01T09:00:00Z` |
| `createdAt` / `updatedAt` | ISO-8601 date-time | Set automatically |

### Examples

```bash
# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title": "Review case bundle", "description": "Check all evidence is uploaded", "dueDate": "2026-12-01T09:00:00Z"}'

# Get the first page of 10 tasks
curl 'http://localhost:8080/api/tasks?page=0&size=10'

# Update a task's status
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status": "INPROGRESS"}'

# Delete a task
curl -X DELETE http://localhost:8080/api/tasks/1
```

Paged response shape:

```json
{
  "content": [ { "id": 1, "title": "Review case bundle", "status": "CREATED", "...": "..." } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

Paging notes: `page` defaults to `0` and `size` to `20`; `size` is capped at `100`. Results always use the default
ordering (due date, then creation time).

## Error handling

All errors are returned with problem details with content type
`application/problem+json`:

```json
{
  "title": "Validation error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/tasks",
  "errors": {
    "title": "Title is required",
    "dueDate": "Due date must be in the future"
  }
}
```

| Status | When |
| --- | --- |
| `400 Validation error` | A field fails validation; `errors` maps each field to its message |
| `400 Malformed request` | Invalid JSON, unknown status value or unparseable date |
| `400 Invalid parameter` | Non-numeric task id in the path |
| `404 Task not found` | No task with the given id |
| `404` / `405` / `415` | Unknown route, unsupported HTTP method, unsupported content type |

The frontend validates the create form with the same rules before sending, and shows any server-side field errors
next to the relevant field.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `Could not resolve placeholder 'DB_HOST'` on startup | `backend/.env` is missing, or the backend was not started from `backend/` |
| `docker compose up` fails with `DB_NAME must be set in backend/.env` | Create `backend/.env` (see [Configuration](#configuration-env)) |
| Backend cannot connect / authentication failed | Check the container is running (`docker compose ps`) and `.env` matches the values the volume was created with; to start fresh run `docker compose down -v && docker compose up -d` (deletes all data) |
| Port `5432`, `8080` or `5173` already in use | Stop the other process, or change `DB_PORT` in `.env` / the Vite port |
| Backend tests fail to start containers | Make sure Docker is running |
| Flyway reports a checksum mismatch after pulling changes | A migration was edited after it ran locally; reset the local database with `docker compose down -v && docker compose up -d` |
| Frontend shows "Could not reach the server" | Start the backend on port 8080 (or set `API_URL`) |
