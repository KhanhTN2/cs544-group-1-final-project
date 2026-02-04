# Final Report

## 1) Executive Summary
This project delivers an event-driven release management platform composed of five Spring Boot microservices, one React frontend, Kafka for asynchronous workflows, MongoDB for persistence, and Grafana/Prometheus for observability.

Primary outcome:
- A complete task orchestration lifecycle (create release -> assign tasks -> start/complete -> release completion)
- Real-time collaboration (discussion + shared activity stream)
- AI assistant with chat history and retrieval context
- Notification and operational alerting pipeline with DLQ handling

## 2) System Scope
In-scope domains:
- Authentication and authorization
- Release and task workflow control
- Discussion and threaded comments
- AI-driven assistant interactions
- Notification and incident signaling
- Monitoring dashboards and metrics

Out-of-scope:
- Multi-region deployment
- API gateway/service mesh
- Distributed transaction manager

## 3) Architecture Decisions
1. Microservice split by bounded context:
- `auth-service`
- `release-service`
- `discussion-service`
- `ai-chat-service`
- `notification-service`

2. Event-driven integration via Kafka:
- Reduces runtime coupling between producer/consumer services
- Enables extensible listeners (activity stream, notifications, alerting)

3. MongoDB document model:
- Fast feature delivery
- Flexible payload evolution
- Supports embedded `tasks` inside `release` aggregate

4. JWT with centralized validation:
- Token issued by auth service
- Downstream services verify token and can optionally call `/auth/validate`

## 4) Key Data and Event Flows
1. Release/task flow:
- Admin creates release and tasks
- Release service emits `TaskAssigned`, `TaskStarted`, `TaskCompleted`, `HotfixTaskAdded`, `StaleTaskDetected`
- Notification service consumes and emails stakeholders

2. Collaboration flow:
- Discussion service stores comments and emits `DiscussionMessageCreated`
- Activity stream service consumes events and publishes SSE updates to frontend

3. AI flow:
- AI chat service stores messages, calls Ollama, emits `AiChatResponse`
- Conversation history is queryable by user/conversation

4. Error flow:
- Services publish `SystemErrorEvent` to `system.errors`
- Notification service logs and emails incident recipients
- DLQ listeners publish alerts for failed message processing

## 5) APIs and Security Model
- Role model:
  - `ADMIN`: release creation, task assignment, release completion
  - `DEVELOPER`: task execution, discussion, AI chat, notifications stream
- Auth endpoints:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /auth/validate`

Representative domain endpoints:
- Release: `POST /api/releases`, `POST /api/releases/{id}/tasks`, `PATCH /api/tasks/{id}/start`
- Discussion: `POST /tasks/{taskId}/comments`, `GET /api/activity/stream`
- AI Chat: `POST /api/chat`, `GET /api/chat/conversations`
- Notification: `POST /api/notifications/system-error`, `GET /api/notifications/stream`

## 6) Observability and Operations
- Metrics scraping through `/actuator/prometheus` from:
  - `release-service`
  - `discussion-service`
  - `ai-chat-service`
  - `notification-service`
- Grafana dashboard:
  - `monitoring/grafana/dashboards/system-overview.json`
- Alert support:
  - Prometheus alert rules + system error topic + email alerting

## 7) Risks and Tradeoffs
1. Shared infrastructure tradeoff:
- Single Kafka/Mongo simplifies local deployment
- Increased blast radius for production-scale failures

2. Embedded task model tradeoff:
- Simple aggregate updates
- Harder to query tasks at very large scale vs dedicated task collection

3. Event ordering and idempotency:
- Current design relies on consumer logic and DLQ
- Production hardening should add explicit idempotency keys/checks

4. AI dependency:
- Ollama model latency can dominate end-user response times

## 8) Future Improvements
1. Add an API gateway and unified rate limiting.
2. Introduce OpenTelemetry traces across HTTP + Kafka hops.
3. Add explicit Mongo indexes through migration scripts.
4. Add contract tests for Kafka event schemas.
5. Add outbox pattern for stronger delivery guarantees.

## 9) Document Map
- System architecture: `docs/architecture.md`
- ERD: `docs/erd.md`
- Mongo schema: `docs/schema.md`
- Event contracts: `docs/event-contracts.md`
