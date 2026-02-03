package com.cs544.discussion.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cs544.discussion.model.DiscussionMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ActivityStreamService {
    private final Sinks.Many<ActivityEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ActivityEvent> stream() {
        return sink.asFlux();
    }

    public void emit(ActivityEvent event) {
        sink.tryEmitNext(event);
    }

    public void emitDiscussion(DiscussionMessage message) {
        emit(new ActivityEvent(
                "DiscussionMessageCreated",
                "New discussion message from " + message.getAuthor(),
                Instant.now(),
                Map.of(
                        "releaseId", message.getReleaseId(),
                        "taskId", message.getTaskId(),
                        "messageId", message.getId()
                )
        ));
    }

    public record ActivityEvent(String type, String message, Instant timestamp, Map<String, Object> payload) {
    }
}
