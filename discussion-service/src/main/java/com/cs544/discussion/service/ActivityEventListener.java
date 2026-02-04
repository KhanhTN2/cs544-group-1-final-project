package com.cs544.discussion.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cs544.discussion.event.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ActivityEventListener {
    private final ObjectMapper objectMapper;
    private final ActivityStreamService activityStreamService;

    public ActivityEventListener(ObjectMapper objectMapper, ActivityStreamService activityStreamService) {
        this.objectMapper = objectMapper;
        this.activityStreamService = activityStreamService;
    }

    @KafkaListener(topics = "release.events", groupId = "activity-stream")
    public void consumeReleaseEvents(EventEnvelope<?> envelope) {
        if (envelope == null) {
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
        }
    }

    @KafkaListener(topics = "discussion.events", groupId = "activity-stream")
    public void consumeDiscussionEvents(EventEnvelope<?> envelope) {
        if (envelope == null) {
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
    }
}
