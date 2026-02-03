package com.cs544.release.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.release.event.EventEnvelope;
import com.cs544.release.event.HotfixTaskAddedEvent;
import com.cs544.release.event.StaleTaskReminderEvent;
import com.cs544.release.event.TaskAssignedEvent;
import com.cs544.release.model.Release;
import com.cs544.release.model.Task;

@Service
public class ReleaseEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReleaseEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
    }

    public void publishStaleTaskReminder(Release release, Task task) {
        StaleTaskReminderEvent payload = new StaleTaskReminderEvent(
                task.getAssigneeId(),
                release.getId(),
                task.getId(),
                task.getTitle(),
                task.getUpdatedAt()
        );
        EventEnvelope<StaleTaskReminderEvent> envelope = new EventEnvelope<>(
                "StaleTaskReminder",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("release.events", envelope.eventType(), envelope);
    }
}
