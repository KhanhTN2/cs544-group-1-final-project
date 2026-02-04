package com.cs544.discussion.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.cs544.discussion.event.EventEnvelope;
import com.cs544.discussion.event.SystemErrorEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ActivityEventListener {
    private final ObjectMapper objectMapper;
    private final ActivityStreamService activityStreamService;
    private final SystemErrorPublisher systemErrorPublisher;

    public ActivityEventListener(
            ObjectMapper objectMapper,
            ActivityStreamService activityStreamService,
            SystemErrorPublisher systemErrorPublisher
    ) {
        this.objectMapper = objectMapper;
        this.activityStreamService = activityStreamService;
        this.systemErrorPublisher = systemErrorPublisher;
    }

    @KafkaListener(topics = "release.events", groupId = "activity-stream")
    public void consumeReleaseEvents(EventEnvelope<?> envelope, Acknowledgment acknowledgment) {
        if (envelope == null) {
            acknowledgment.acknowledge();
            return;
        }
        String eventType = envelope.eventType();
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        if ("TaskStarted".equals(eventType)) {
            activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                    "TaskStarted",
                    "Task started by " + payload.get("developerId"),
                    Instant.now(),
                    payload
            ));
        } else if ("TaskCompleted".equals(eventType)) {
            activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                    "TaskCompleted",
                    "Task completed by " + payload.get("developerId"),
                    Instant.now(),
                    payload
            ));
        } else if ("HotfixTaskAdded".equals(eventType)) {
            activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                    "HotfixTaskAdded",
                    "Hotfix task added to release " + payload.get("releaseId"),
                    Instant.now(),
                    payload
            ));
        } else if ("StaleTaskDetected".equals(eventType)) {
            activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                    "StaleTaskDetected",
                    "Stale task detected for " + payload.get("developerId"),
                    Instant.now(),
                    payload
            ));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "discussion.events", groupId = "activity-stream")
    public void consumeDiscussionEvents(EventEnvelope<?> envelope, Acknowledgment acknowledgment) {
        if (envelope == null) {
            acknowledgment.acknowledge();
            return;
        }
        String eventType = envelope.eventType();
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        if ("DiscussionMessageCreated".equals(eventType)) {
            String author = String.valueOf(payload.get("author"));
            String content = String.valueOf(payload.get("message"));
            String summary = content == null ? "" : content.trim();
            if (summary.length() > 140) {
                summary = summary.substring(0, 140) + "...";
            }
            activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                    "DiscussionMessageCreated",
                    "New message from " + author + ": " + summary,
                    Instant.now(),
                    payload
            ));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"release.events.DLQ", "discussion.events.DLQ", "system.errors.DLQ"}, groupId = "activity-stream-dlq")
    public void consumeDeadLetterEvents(
            EventEnvelope<?> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String failureReason,
            Acknowledgment acknowledgment
    ) {
        if (envelope != null) {
            String reason = failureReason == null || failureReason.isBlank() ? "unknown" : failureReason;
            String message = String.format(
                    "DLQ event detected on %s (eventType=%s, eventId=%s, reason=%s)",
                    topic,
                    envelope.eventType(),
                    envelope.id(),
                    reason
            );
            systemErrorPublisher.publish(message);
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "system.errors", groupId = "activity-stream")
    public void consumeSystemErrors(EventEnvelope<?> envelope, Acknowledgment acknowledgment) {
        if (envelope == null || !"SystemErrorEvent".equals(envelope.eventType())) {
            acknowledgment.acknowledge();
            return;
        }
        SystemErrorEvent event = objectMapper.convertValue(envelope.payload(), SystemErrorEvent.class);
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        activityStreamService.emit(new ActivityStreamService.ActivityEvent(
                "SystemErrorEvent",
                "System alert from " + event.service() + ": " + event.message(),
                Instant.now(),
                payload
        ));
        acknowledgment.acknowledge();
    }
}
