package com.cs544.notification.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.notification.event.EventEnvelope;

@Service
public class NotificationEventProducer {
    private final KafkaTemplate<String, EventEnvelope<SystemErrorEvent>> kafkaTemplate;

    public NotificationEventProducer(KafkaTemplate<String, EventEnvelope<SystemErrorEvent>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSystemError(SystemErrorEvent event) {
        EventEnvelope<SystemErrorEvent> envelope = new EventEnvelope<>(
                "SystemErrorEvent",
                "notification-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                event
        );
        kafkaTemplate.send("system.errors", envelope.eventType(), envelope);
    }
}
