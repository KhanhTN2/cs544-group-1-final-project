package com.cs544.release.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.release.event.EventEnvelope;
import com.cs544.release.event.SystemErrorEvent;

@Service
public class SystemErrorPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SystemErrorPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String message) {
        SystemErrorEvent payload = new SystemErrorEvent("release-service", message);
        EventEnvelope<SystemErrorEvent> envelope = new EventEnvelope<>(
                "SystemErrorEvent",
                "release-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                payload
        );
        kafkaTemplate.send("system.errors", envelope.eventType(), envelope);
    }
}
