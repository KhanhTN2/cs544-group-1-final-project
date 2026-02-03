package com.cs544.release.model;

import java.time.Instant;
import java.util.UUID;

public class Task {
    private String id;
    private String title;
    private String description;
    private String assigneeId;
    private int orderIndex;
    private TaskStatus status = TaskStatus.TODO;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Task() {
    }

    public Task(String title, String description, String assigneeId, int orderIndex) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.orderIndex = orderIndex;
        this.status = TaskStatus.TODO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
