package com.cs544.notification.service;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cs544.notification.event.EventEnvelope;
import com.cs544.notification.event.HotfixTaskAddedEvent;
import com.cs544.notification.event.StaleTaskDetectedEvent;
import com.cs544.notification.event.TaskAssignedEvent;
import com.cs544.notification.event.TaskCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationEventHandler eventHandler;
    private final SystemErrorStreamService streamService;

    public NotificationEventListener(
            ObjectMapper objectMapper,
            NotificationEventHandler eventHandler,
            SystemErrorStreamService streamService
    ) {
        this.objectMapper = objectMapper;
        this.eventHandler = eventHandler;
        this.streamService = streamService;
    }

    @KafkaListener(topics = "system.errors", groupId = "notification-service")
    public void consumeSystemErrors(EventEnvelope<?> envelope) {
        if (envelope == null) {
            return;
        }
        if (!"SystemErrorEvent".equals(envelope.eventType())) {
            return;
        }
        SystemErrorEvent event = objectMapper.convertValue(envelope.payload(), SystemErrorEvent.class);
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        eventHandler.handleSystemError(envelope.source(), envelope.id(), event, payload);
        streamService.recordError(envelope.id(), event);
    }

    @KafkaListener(topics = "release.events", groupId = "notification-service")
    public void consumeReleaseEvents(EventEnvelope<?> envelope) {
        if (envelope == null) {
            return;
        }
        String eventType = envelope.eventType();
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        if ("TaskAssigned".equals(eventType)) {
            TaskAssignedEvent event = objectMapper.convertValue(envelope.payload(), TaskAssignedEvent.class);
            eventHandler.handleTaskAssigned(envelope.source(), envelope.id(), event, payload);
        } else if ("HotfixTaskAdded".equals(eventType)) {
            HotfixTaskAddedEvent event = objectMapper.convertValue(envelope.payload(), HotfixTaskAddedEvent.class);
            eventHandler.handleHotfixTaskAdded(envelope.source(), envelope.id(), event, payload);
        } else if ("StaleTaskDetected".equals(eventType)) {
            StaleTaskDetectedEvent event = objectMapper.convertValue(envelope.payload(), StaleTaskDetectedEvent.class);
            eventHandler.handleStaleTaskDetected(envelope.source(), envelope.id(), event, payload);
        } else if ("TaskCompleted".equals(eventType)) {
            objectMapper.convertValue(envelope.payload(), TaskCompletedEvent.class);
        }
    }
}
