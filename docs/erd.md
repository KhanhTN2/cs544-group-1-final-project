# ERD (Logical)

The platform is MongoDB-based with service-owned data stores. Relationships below are logical (application-level), not database-enforced foreign keys.

## Entity Relationship Diagram
```mermaid
erDiagram
    USER {
        string id PK
        string username UK
        string passwordHash
        string role
    }

    RELEASE {
        string id PK
        string name
        string version
        datetime createdAt
        bool completed
        datetime completedAt
        datetime lastCompletedAt
    }

    TASK {
        string id PK
        string title
        string description
        string assigneeId
        int orderIndex
        string status
        datetime createdAt
        datetime updatedAt
    }

    DISCUSSION_MESSAGE {
        string id PK
        string releaseId
        string taskId
        string parentId
        string author
        string message
        datetime createdAt
    }

    CHAT_MESSAGE {
        string id PK
        string userId
        string conversationId
        string role
        string content
        string format
        datetime createdAt
    }

    NOTIFICATION_LOG {
        string id PK
        string eventType
        string recipient
        string subject
        string body
        string status
        string error
        string source
        string eventId
        datetime createdAt
        map payload
    }

    USER ||--o{ RELEASE : "admin creates"
    RELEASE ||--|{ TASK : "embeds"
    USER ||--o{ TASK : "assigneeId"
    RELEASE ||--o{ DISCUSSION_MESSAGE : "releaseId"
    TASK ||--o{ DISCUSSION_MESSAGE : "taskId"
    DISCUSSION_MESSAGE ||--o{ DISCUSSION_MESSAGE : "parentId (threaded replies)"
    USER ||--o{ CHAT_MESSAGE : "userId"
    USER ||--o{ NOTIFICATION_LOG : "recipient (email/user mapping)"
    TASK ||--o{ NOTIFICATION_LOG : "event payload refs"
```

## Notes
- `TASK` is embedded inside the `RELEASE` document in MongoDB.
- `DISCUSSION_MESSAGE.parentId` supports nested reply threads.
- `NOTIFICATION_LOG.payload` stores the original event payload for traceability.
- AI chat conversations are grouped by `conversationId` and ordered by `createdAt`.
