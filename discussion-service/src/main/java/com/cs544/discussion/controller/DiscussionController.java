package com.cs544.discussion.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import com.cs544.discussion.model.DiscussionMessage;
import com.cs544.discussion.repository.DiscussionRepository;
import com.cs544.discussion.service.DiscussionEventProducer;
import com.cs544.discussion.service.ActivityStreamService;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionRepository repository;
    private final DiscussionEventProducer eventProducer;
    private final ActivityStreamService activityStreamService;

    public DiscussionController(
            DiscussionRepository repository,
            DiscussionEventProducer eventProducer,
            ActivityStreamService activityStreamService
    ) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.activityStreamService = activityStreamService;
    }

    @PostMapping
    public ResponseEntity<?> createMessage(@RequestBody DiscussionRequest request) {
        if (request.taskId() == null || request.taskId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("taskId is required."));
        }
        DiscussionMessage message = repository.save(new DiscussionMessage(
                request.releaseId(),
                request.taskId(),
                request.parentId(),
                request.author(),
                request.message()
        ));
        eventProducer.publishMessageCreated(message);
        activityStreamService.emitDiscussion(message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{releaseId}")
    public ResponseEntity<List<DiscussionMessage>> listMessages(@PathVariable String releaseId) {
        return ResponseEntity.ok(repository.findByReleaseId(releaseId));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<List<DiscussionMessage>> listTaskMessages(@PathVariable String taskId) {
        return ResponseEntity.ok(repository.findByTaskId(taskId));
    }

    @GetMapping(path = "/stream/{releaseId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DiscussionMessage> streamMessages(@PathVariable String releaseId) {
        return Flux.fromIterable(repository.findByReleaseId(releaseId));
    }

    @GetMapping(path = "/stream/tasks/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DiscussionMessage> streamTaskMessages(@PathVariable String taskId) {
        return Flux.fromIterable(repository.findByTaskId(taskId));
    }

    public record DiscussionRequest(String releaseId, String taskId, String parentId, String author, String message) {
    }

    public record ErrorResponse(String message) {
    }
}
