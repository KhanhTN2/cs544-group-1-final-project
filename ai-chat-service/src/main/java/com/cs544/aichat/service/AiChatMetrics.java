package com.cs544.aichat.service;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class AiChatMetrics {
    private final Counter requests;
    private final Timer latency;
    private final MeterRegistry meterRegistry;

    public AiChatMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requests = Counter.builder("ai_chat_requests_total")
                .description("Total AI chat requests")
                .register(meterRegistry);
        this.latency = Timer.builder("ai_chat_request_latency")
                .description("AI chat request latency")
                .register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        requests.increment();
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(latency);
    }

    public void incrementKafkaEvent(String eventType) {
        meterRegistry.counter("kafka_events_published_total", "event", eventType).increment();
    }
}
