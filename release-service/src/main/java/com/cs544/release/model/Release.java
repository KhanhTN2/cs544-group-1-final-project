package com.cs544.release.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "releases")
public class Release {
    @Id
    private String id;
    private String name;
    private String version;
    private Instant createdAt = Instant.now();
    private boolean completed;
    private Instant completedAt;
    private Instant lastCompletedAt;
    private List<Task> tasks = new ArrayList<>();

    public Release() {
    }

    public Release(String name, String version) {
        this.name = name;
        this.version = version;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void setLastCompletedAt(Instant lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
