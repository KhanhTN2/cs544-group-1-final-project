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

    public DiscussionEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
    }
}
