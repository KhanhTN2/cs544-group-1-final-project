# Event Contracts

## Envelope
All events share a standard envelope:

```json
{
  "eventType": "<Type>",
  "source": "<service>",
  "id": "<uuid>",
  "timestamp": "<ISO-8601>",
  "headers": {
    "schema": "v1"
  },
  "payload": {}
}
```

## Events

### ReleaseCreated
Topic: `release.events`

Payload:
```json
{
  "id": "<release id>",
  "name": "<name>",
  "version": "<version>",
  "createdAt": "<ISO-8601>"
}
```

### TaskAssigned
Topic: `release.events`

Payload:
```json
{
  "developerId": "<developer id>",
  "releaseId": "<release id>",
  "taskId": "<task id>",
  "taskTitle": "<task title>"
}
```

### TaskStarted
Topic: `release.events`

Payload:
```json
{
  "developerId": "<developer id>",
  "releaseId": "<release id>",
  "taskId": "<task id>",
  "taskTitle": "<task title>"
}
```

### TaskCompleted
Topic: `release.events`

Payload:
```json
{
  "developerId": "<developer id>",
  "releaseId": "<release id>",
  "taskId": "<task id>",
  "taskTitle": "<task title>"
}
```

### HotfixTaskAdded
Topic: `release.events`

Payload:
```json
{
  "developerId": "<developer id>",
  "releaseId": "<release id>",
  "taskTitle": "<task title>"
}
```

### StaleTaskDetected
Topic: `release.events`

Payload:
```json
{
  "developerId": "<developer id>",
  "releaseId": "<release id>",
  "taskId": "<task id>",
  "taskTitle": "<task title>",
  "lastUpdatedAt": "<ISO-8601>"
}
```

### DiscussionMessageCreated
Topic: `discussion.events`

Payload:
```json
{
  "id": "<message id>",
  "releaseId": "<release id>",
  "author": "<author>",
  "message": "<message>",
  "createdAt": "<ISO-8601>"
}
```

### AiChatResponse
Topic: `ai-chat.events`

Payload:
```json
{
  "prompt": "<prompt>",
  "reply": "<reply>"
}
```

### SystemErrorEvent
Topic: `system.errors`

Payload:
```json
{
  "service": "<service>",
  "message": "<error message>"
}
```
