package com.cs544.discussion.service;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cs544.discussion.model.DiscussionMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ActivityStreamService {
    // Replay recent activity events to new subscribers so everyone sees shared history.
    private final Sinks.Many<ActivityEvent> sink = Sinks.many().replay().limit(400);

    public Flux<ActivityEvent> stream() {
        Flux<ActivityEvent> heartbeat = Flux.interval(Duration.ofSeconds(8))
                .map(tick -> new ActivityEvent("heartbeat", "", Instant.now(), Map.of()));
        return Flux.merge(sink.asFlux(), heartbeat);
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
