# System Architecture

## 1) Problem Frame
Build a release orchestration platform with:
- Role-based authentication (`ADMIN`, `DEVELOPER`)
- Task lifecycle and ordering controls
- Cross-service activity stream (event-driven)
- AI chat assistant with persisted conversation history
- Notification + DLQ/system-error alerting
- Observability with Prometheus + Grafana

## 2) High-Level Architecture
```mermaid
flowchart LR
    U[Web Frontend<br/>Port 8085]

    subgraph SVC[Domain Services]
        AUTH[Auth Service<br/>8086]
        REL[Release Service<br/>8081]
        DISC[Discussion Service<br/>8082]
        AI[AI Chat Service<br/>8083]
        NOTIF[Notification Service<br/>8084]
    end

    subgraph DATA[Data + Messaging]
        MDB[(MongoDB)]
        KAFKA[(Kafka)]
        OLLAMA[Ollama LLM<br/>11434]
        MAIL[MailHog SMTP<br/>1025/8025]
    end

    subgraph OBS[Observability]
        PROM[Prometheus]
        GRAF[Grafana]
    end

    U --> AUTH
    U --> REL
    U --> DISC
    U --> AI
    U --> NOTIF

    REL --> AUTH
    DISC --> AUTH
    AI --> AUTH
    NOTIF --> AUTH

    AUTH --> MDB
    REL --> MDB
    DISC --> MDB
    AI --> MDB
    NOTIF --> MDB

    REL --> KAFKA
    DISC --> KAFKA
    AI --> KAFKA
    NOTIF --> KAFKA
    KAFKA --> DISC
    KAFKA --> NOTIF

    AI --> OLLAMA
    NOTIF --> MAIL

    PROM --> REL
    PROM --> DISC
    PROM --> AI
    PROM --> NOTIF
    GRAF --> PROM
```

## 3) Event-Driven Backbone
```mermaid
flowchart LR
    REL[Release Service] -->|release.events| K[(Kafka)]
    DISC[Discussion Service] -->|discussion.events| K
    AI[AI Chat Service] -->|ai-chat.events| K
    NOTIF[Notification Service] -->|system.errors| K

    K -->|release.events + discussion.events + system.errors| ACT[Discussion Activity Stream]
    K -->|release.events + system.errors| NPROC[Notification Event Handler]

    K -->|*.DLQ| DLQH["DLQ Consumers<br>Discussion + Notification"]
    DLQH -->|system.errors| K
```

Core topics:
- `release.events`
- `discussion.events`
- `ai-chat.events`
- `system.errors`
- Dead-letter topics: `release.events.DLQ`, `discussion.events.DLQ`, `system.errors.DLQ`

## 4) Trust and Security Boundary
- JWT issued by `auth-service` (`/auth/login`, `/auth/register`)
- Service-level token validation against `auth-service /auth/validate` (enabled by `AUTH_VALIDATE=true`)
- Role checks via method security (`@PreAuthorize`) on privileged endpoints

## 5) Deployment View (Current)
- Single Docker Compose stack
- Shared Kafka + shared MongoDB server, with logical DB-per-service separation:
  - `auth`
  - `releases`
  - `discussions`
  - `ai-chat`
  - `notifications`

## 6) Non-Functional Notes
- Horizontal scaling candidate services: `release-service`, `discussion-service`, `notification-service`
- Main bottlenecks to watch: Kafka partitioning strategy, Mongo query/index coverage, Ollama response latency
- Observability baseline already wired (`/actuator/prometheus` for core services)

## 7) Related Documents
- Data model: `docs/erd.md`
- Mongo schema details: `docs/schema.md`
- System rationale and outcomes: `docs/final-report.md`
- Event contracts: `docs/event-contracts.md`
