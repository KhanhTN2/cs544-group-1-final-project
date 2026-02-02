# CS544 Group 1 Final Project

## Prerequisites
- Docker + Docker Compose
- Java 21 (for local builds)

## Run
```bash
mvn -f release-service/pom.xml package
mvn -f discussion-service/pom.xml package
mvn -f ai-chat-service/pom.xml package
mvn -f notification-service/pom.xml package

docker compose up --build
```

## Demo Script
```bash
# Tokens
curl -X POST localhost:8081/api/releases/token -H 'Content-Type: application/json' -d '{"username":"demo"}'
curl -X POST localhost:8082/api/discussions/token -H 'Content-Type: application/json' -d '{"username":"demo"}'
curl -X POST localhost:8083/api/chat/token -H 'Content-Type: application/json' -d '{"username":"demo"}'
curl -X POST localhost:8084/api/notifications/token -H 'Content-Type: application/json' -d '{"username":"demo"}'

# Create release
curl -X POST localhost:8081/api/releases -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{"name":"Album","version":"1.0"}'

# Create discussion message
curl -X POST localhost:8082/api/discussions -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{"releaseId":"rel-1","author":"sam","message":"hello"}'

# SSE stream
curl -N -H 'Authorization: Bearer <token>' localhost:8082/api/discussions/stream/rel-1

# AI chat
curl -X POST localhost:8083/api/chat -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{"prompt":"Summarize"}'

# System error event
curl -X POST localhost:8084/api/notifications/system-error -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{"service":"release-service","message":"down"}'
```
