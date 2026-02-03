package com.cs544.discussion.service;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class DiscussionMetrics {
    private final MeterRegistry meterRegistry;

    public DiscussionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementKafkaEvent(String eventType) {
        meterRegistry.counter("kafka_events_published_total", "event", eventType).increment();
    }
}
