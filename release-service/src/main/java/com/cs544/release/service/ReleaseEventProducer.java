package com.cs544.release.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.release.event.EventEnvelope;
import com.cs544.release.event.HotfixTaskAddedEvent;
import com.cs544.release.event.StaleTaskDetectedEvent;
import com.cs544.release.event.TaskAssignedEvent;
import com.cs544.release.event.TaskCompletedEvent;
import com.cs544.release.event.TaskStartedEvent;
import com.cs544.release.model.Release;
import com.cs544.release.model.Task;

@Service
public class ReleaseEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReleaseMetrics metrics;

    public ReleaseEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ReleaseMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    public void publishReleaseCreated(Release release) {
        EventEnvelope<Release> envelope = new EventEnvelope<>(
                "ReleaseCreated",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                release
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public void publishHotfixTaskAdded(Release release, Task task) {
        HotfixTaskAddedEvent payload = new HotfixTaskAddedEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getTitle()
        );
        EventEnvelope<HotfixTaskAddedEvent> envelope = new EventEnvelope<>(
                "HotfixTaskAdded",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public void publishTaskAssigned(Release release, Task task) {
        TaskAssignedEvent payload = new TaskAssignedEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getId(),
                task.getTitle()
        );
        EventEnvelope<TaskAssignedEvent> envelope = new EventEnvelope<>(
                "TaskAssigned",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public void publishTaskStarted(Release release, Task task) {
        TaskStartedEvent payload = new TaskStartedEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getId(),
                task.getTitle()
        );
        EventEnvelope<TaskStartedEvent> envelope = new EventEnvelope<>(
                "TaskStarted",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public void publishTaskCompleted(Release release, Task task) {
        TaskCompletedEvent payload = new TaskCompletedEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getId(),
                task.getTitle()
        );
        EventEnvelope<TaskCompletedEvent> envelope = new EventEnvelope<>(
                "TaskCompleted",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public void publishStaleTaskDetected(Release release, Task task) {
        StaleTaskDetectedEvent payload = new StaleTaskDetectedEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getId(),
                task.getTitle(),
                task.getUpdatedAt()
        );
        EventEnvelope<StaleTaskDetectedEvent> envelope = new EventEnvelope<>(
                "StaleTaskDetected",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }
}
