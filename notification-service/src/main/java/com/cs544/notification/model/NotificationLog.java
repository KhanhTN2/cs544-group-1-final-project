package com.cs544.notification.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "NotificationLog")
public class NotificationLog {
    @Id
    private String id;
    private String eventType;
    private String recipient;
    private String subject;
    private String body;
    private String status;
    private String error;
    private String source;
    private String eventId;
    private Instant createdAt;
    private Map<String, Object> payload;

    public NotificationLog() {
    }

    public NotificationLog(
            String eventType,
            String recipient,
            String subject,
            String body,
            String status,
            String error,
            String source,
            String eventId,
            Instant createdAt,
            Map<String, Object> payload
    ) {
        this.eventType = eventType;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.error = error;
        this.source = source;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getSource() {
        return source;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
