# MongoDB Schema (Current + Recommended Indexes)

## 1) `auth.users`
Collection: `users`

```json
{
  "_id": "66f2f2e9c31d6d4eb66c0001",
  "username": "admin",
  "passwordHash": "$2a$10$...",
  "role": "ADMIN"
}
```

Indexes:
- Current (required by query patterns):
  - `{ username: 1 }` unique

---

## 2) `releases.releases`
Collection: `releases`

```json
{
  "_id": "rel-1001",
  "name": "Album",
  "version": "1.0",
  "createdAt": "2026-02-04T10:00:00Z",
  "completed": false,
  "completedAt": null,
  "lastCompletedAt": null,
  "tasks": [
    {
      "id": "task-1",
      "title": "Implement auth",
      "description": "JWT + role checks",
      "assigneeId": "dev-1",
      "orderIndex": 1,
      "status": "IN_PROCESS",
      "createdAt": "2026-02-04T10:01:00Z",
      "updatedAt": "2026-02-04T10:15:00Z"
    }
  ]
}
```

Indexes:
- Current (recommended for repository queries):
  - `{ "tasks.id": 1 }`
  - `{ "tasks.assigneeId": 1 }`
  - `{ "tasks.assigneeId": 1, "tasks.status": 1 }`

---

## 3) `discussions.discussion_messages`
Collection: `discussion_messages`

```json
{
  "_id": "msg-9001",
  "releaseId": "rel-1001",
  "taskId": "task-1",
  "parentId": null,
  "author": "dev-1",
  "message": "Started implementation today.",
  "createdAt": "2026-02-04T10:20:00Z"
}
```

Indexes:
- Current (recommended for query patterns):
  - `{ taskId: 1, createdAt: 1 }`
  - `{ releaseId: 1, createdAt: 1 }`
  - `{ parentId: 1 }`

---

## 4) `ai-chat.ChatMessage`
Collection: `ChatMessage`

```json
{
  "_id": "chat-501",
  "userId": "dev-1",
  "conversationId": "conv-20260204-01",
  "role": "USER",
  "content": "Summarize blockers for release rel-1001",
  "format": "text",
  "createdAt": "2026-02-04T10:25:00Z"
}
```

Indexes:
- Current (recommended for repository queries):
  - `{ userId: 1, createdAt: -1 }`
  - `{ userId: 1, conversationId: 1, createdAt: -1 }`
  - `{ userId: 1, conversationId: 1, createdAt: 1 }`

---

## 5) `notifications.NotificationLog`
Collection: `NotificationLog`

```json
{
  "_id": "notif-7001",
  "eventType": "TaskAssigned",
  "recipient": "dev-1@cs544.local",
  "subject": "New task assigned: Implement auth",
  "body": "You have been assigned a new task...",
  "status": "SENT",
  "error": null,
  "source": "release-service",
  "eventId": "0ef2a06f-9f95-4e22-bdbf-9f3cf1f7e9e2",
  "createdAt": "2026-02-04T10:26:00Z",
  "payload": {
    "developerId": "dev-1",
    "releaseId": "rel-1001",
    "taskId": "task-1",
    "taskTitle": "Implement auth"
  }
}
```

Indexes:
- Recommended:
  - `{ eventId: 1 }`
  - `{ eventType: 1, createdAt: -1 }`
  - `{ recipient: 1, createdAt: -1 }`
  - `{ status: 1, createdAt: -1 }`

---

## 6) Cross-Service Data Contracts
- Logical key links:
  - `Task.assigneeId` -> `User.username`
  - `DiscussionMessage.releaseId` -> `Release._id`
  - `DiscussionMessage.taskId` -> `Release.tasks.id`
  - `ChatMessage.userId` -> `User.username`
  - `NotificationLog.payload.*` -> event contract fields
- Integrity is enforced at service layer, not by MongoDB foreign keys.
