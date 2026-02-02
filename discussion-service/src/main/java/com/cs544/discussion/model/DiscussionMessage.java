package com.cs544.discussion.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "discussion_messages")
public class DiscussionMessage {
    @Id
    private String id;
    private String releaseId;
    private String author;
    private String message;
    private Instant createdAt = Instant.now();

    public DiscussionMessage() {
    }

    public DiscussionMessage(String releaseId, String author, String message) {
        this.releaseId = releaseId;
        this.author = author;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
