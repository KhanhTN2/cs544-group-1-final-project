package com.cs544.notification.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cs544.notification.event.EventEnvelope;
import com.cs544.notification.event.HotfixTaskAddedEvent;
import com.cs544.notification.event.StaleTaskDetectedEvent;
import com.cs544.notification.event.TaskAssignedEvent;

@Service
public class NotificationEventHandler {
    private final NotificationEmailService emailService;

    public NotificationEventHandler(NotificationEmailService emailService) {
        this.emailService = emailService;
    }

    public void handleTaskAssigned(String source, String eventId, TaskAssignedEvent event, Map<String, Object> payload) {
        String recipient = emailService.resolveDeveloperEmail(event.developerId());
        String subject = "New task assigned: " + event.taskTitle();
        String body = String.join("\n",
                "You have been assigned a new task.",
                "Release: " + event.releaseId(),
                "Task: " + event.taskTitle(),
                "Task ID: " + event.taskId(),
                "Assignee: " + event.developerId()
        );
        emailService.send("TaskAssigned", source, eventId, List.of(recipient), subject, body, payload);
    }

    public void handleHotfixTaskAdded(String source, String eventId, HotfixTaskAddedEvent event, Map<String, Object> payload) {
        String recipient = emailService.resolveDeveloperEmail(event.developerId());
        String subject = "Hotfix task added to release " + event.releaseId();
        String body = String.join("\n",
                "A hotfix task was added to a completed release.",
                "Release: " + event.releaseId(),
                "Task: " + event.taskTitle(),
                "Assignee: " + event.developerId()
        );
        emailService.send("HotfixTaskAdded", source, eventId, List.of(recipient), subject, body, payload);
    }

    public void handleStaleTaskDetected(String source, String eventId, StaleTaskDetectedEvent event, Map<String, Object> payload) {
        String recipient = emailService.resolveDeveloperEmail(event.developerId());
        String subject = "Stale task detected: " + event.taskTitle();
        String formattedTime = formatInstant(event.lastUpdatedAt());
        String body = String.join("\n",
                "A task has been in progress without updates for too long.",
                "Release: " + event.releaseId(),
                "Task: " + event.taskTitle(),
                "Task ID: " + event.taskId(),
                "Last updated: " + formattedTime,
                "Assignee: " + event.developerId()
        );
        emailService.send("StaleTaskDetected", source, eventId, List.of(recipient), subject, body, payload);
    }

    public void handleSystemError(String source, String eventId, SystemErrorEvent event, Map<String, Object> payload) {
        String subject = "Critical system error: " + event.service();
        String body = String.join("\n",
                "A critical system error was reported.",
                "Service: " + event.service(),
                "Message: " + event.message(),
                "Reported at: " + formatInstant(Instant.now())
        );
        emailService.send("SystemErrorEvent", source, eventId, emailService.resolveRelatedRecipients(payload), subject, body, payload);
    }

    public void handleDeadLetterEvent(
            String topic,
            EventEnvelope<?> envelope,
            Map<String, Object> payload,
            String failureReason
    ) {
        String reason = failureReason == null || failureReason.isBlank() ? "unknown" : failureReason;
        String subject = "Kafka DLQ event in " + topic;
        String body = String.join("\n",
                "An event was moved to the Dead Letter Queue.",
                "Topic: " + topic,
                "Event type: " + envelope.eventType(),
                "Event id: " + envelope.id(),
                "Source: " + envelope.source(),
                "Failure reason: " + reason,
                "Captured at: " + formatInstant(Instant.now())
        );
        emailService.send("DeadLetterEvent", envelope.source(), envelope.id(), emailService.resolveRelatedRecipients(payload), subject, body, payload);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "unknown";
        }
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(instant);
    }
}
