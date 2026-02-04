# CS544 Group 1 Final Project

Event-driven microservices platform for release orchestration, collaboration, AI chat, and notifications.

## Tech Stack
- Java 21 + Spring Boot
- MongoDB
- Kafka + Zookeeper
- React frontend
- Prometheus + Grafana
- Ollama (local LLM)

## Services and Ports
- `frontend`: `http://localhost:8085`
- `auth-service`: `http://localhost:8086`
- `release-service`: `http://localhost:8081`
- `discussion-service`: `http://localhost:8082`
- `ai-chat-service`: `http://localhost:8083`
- `notification-service`: `http://localhost:8084`
- `grafana`: `http://localhost:3000`
- `prometheus`: `http://localhost:9090`
- `mailhog`: `http://localhost:8025`

## Prerequisites
- Docker + Docker Compose
- Java 21 + Maven (only needed if building jars locally)

## Run
Start everything with Docker:

```bash
docker compose up --build
```

Run tests on demand (separate profile):

```bash
docker compose --profile test up --build --abort-on-container-exit --exit-code-from tests
```

Optional: prebuild service jars before compose:

```bash
mvn -f auth-service/pom.xml package
mvn -f release-service/pom.xml package
mvn -f discussion-service/pom.xml package
mvn -f ai-chat-service/pom.xml package
mvn -f notification-service/pom.xml package
```

## Quick API Smoke Test
```bash
# 1) Register admin and get token
curl -s -X POST http://localhost:8086/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"secret","role":"ADMIN"}'

# 2) Login and export token
TOKEN=$(curl -s -X POST http://localhost:8086/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"secret"}' | jq -r '.token')

# 3) Create release
curl -s -X POST http://localhost:8081/api/releases \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Album","version":"1.0"}'

# 4) Create task on that release (replace <releaseId>)
curl -s -X POST http://localhost:8081/api/releases/<releaseId>/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Implement auth","description":"JWT + roles","assigneeId":"dev-1","orderIndex":1}'

# 5) Emit system error
curl -s -X POST http://localhost:8084/api/notifications/system-error \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"service":"release-service","message":"down"}'
```

## Key API Endpoints
- Auth:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /auth/validate`
- Release:
  - `POST /api/releases`
  - `GET /api/releases`
  - `POST /api/releases/{id}/tasks`
  - `PATCH /api/tasks/{id}/start`
  - `PATCH /api/tasks/{id}/complete`
- Discussion:
  - `POST /tasks/{taskId}/comments`
  - `POST /comments/{commentId}/reply`
  - `GET /tasks/{taskId}/comments`
  - `GET /api/activity/stream`
- AI Chat:
  - `POST /api/chat`
  - `POST /api/chat/stream`
  - `GET /api/chat/conversations`
  - `GET /api/chat/conversations/{conversationId}/messages`
- Notification:
  - `POST /api/notifications/system-error`
  - `GET /api/notifications`
  - `GET /api/notifications/stream`

## Documentation
- Architecture: `docs/architecture.md`
- System architecture alias: `docs/system-architecture.md`
- ERD: `docs/erd.md`
- Schema: `docs/schema.md`
- Event contracts: `docs/event-contracts.md`
- Final report: `docs/final-report.md`
