package com.cs544.aichat.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ChatMessage")
public class ChatMessage {
    @Id
    private String id;
    private String userId;
    private String conversationId;
    private String role;
    private String content;
    private String format;
    private Instant createdAt;

    public ChatMessage() {
    }

    public ChatMessage(String userId, String conversationId, String role, String content, String format, Instant createdAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.format = format;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public String getFormat() {
        return format;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
