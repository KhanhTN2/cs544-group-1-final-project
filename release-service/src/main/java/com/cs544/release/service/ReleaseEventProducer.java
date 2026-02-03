package com.cs544.release.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.release.event.EventEnvelope;
import com.cs544.release.model.Release;

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
}
