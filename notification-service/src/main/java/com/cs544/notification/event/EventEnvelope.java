package com.cs544.notification.event;

import java.time.Instant;
import java.util.Map;

public record EventEnvelope<T>(
        String eventType,
        String source,
        String id,
        Instant timestamp,
        Map<String, String> headers,
        T payload
) {
}
