package com.cs544.discussion.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.discussion.event.EventEnvelope;
import com.cs544.discussion.model.DiscussionMessage;

@Service
public class DiscussionEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DiscussionMetrics metrics;

    public DiscussionEventProducer(KafkaTemplate<String, Object> kafkaTemplate, DiscussionMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    public void publishMessageCreated(DiscussionMessage message) {
        EventEnvelope<DiscussionMessage> envelope = new EventEnvelope<>(
                "DiscussionMessageCreated",
                "discussion-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                message
        );
        kafkaTemplate.send("discussion.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }
}
