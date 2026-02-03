package com.cs544.aichat.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cs544.aichat.event.EventEnvelope;

@Service
public class ChatEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiChatMetrics metrics;

    public ChatEventProducer(KafkaTemplate<String, Object> kafkaTemplate, AiChatMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    public void publishChatResponse(ChatResponse response) {
        EventEnvelope<ChatResponse> envelope = new EventEnvelope<>(
                "AiChatResponse",
                "ai-chat-service",
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of("schema", "v1"),
                response
        );
        kafkaTemplate.send("ai-chat.events", envelope.eventType(), envelope);
        metrics.incrementKafkaEvent(envelope.eventType());
    }

    public record ChatResponse(String prompt, String reply) {
    }
}
