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
        String content = message.getMessage() == null ? "" : message.getMessage().trim();
        String summary = content.length() > 140 ? content.substring(0, 140) + "..." : content;
        emit(new ActivityEvent(
                "DiscussionMessageCreated",
                "New message from " + message.getAuthor() + ": " + summary,
                Instant.now(),
                Map.of(
                        "releaseId", message.getReleaseId(),
                        "taskId", message.getTaskId(),
                        "messageId", message.getId(),
                        "message", message.getMessage()
                )
        ));
    }

    public record ActivityEvent(String type, String message, Instant timestamp, Map<String, Object> payload) {
    }
}
