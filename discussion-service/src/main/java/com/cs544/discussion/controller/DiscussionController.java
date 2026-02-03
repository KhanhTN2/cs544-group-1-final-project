package com.cs544.discussion.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cs544.discussion.model.DiscussionMessage;
import com.cs544.discussion.repository.DiscussionRepository;
import com.cs544.discussion.security.JwtUtil;
import com.cs544.discussion.service.DiscussionEventProducer;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionRepository repository;
    private final DiscussionEventProducer eventProducer;
    private final JwtUtil jwtUtil;
    private static final Set<String> ALLOWED_ROLES = Set.of("release-manager", "dev-1", "dev-2");

    public DiscussionController(DiscussionRepository repository, DiscussionEventProducer eventProducer, JwtUtil jwtUtil) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.jwtUtil = jwtUtil;
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
    public SseEmitter streamMessages(@PathVariable String releaseId) {
        SseEmitter emitter = new SseEmitter();
        repository.findByReleaseId(releaseId).forEach(message -> {
            try {
                emitter.send(message);
            } catch (Exception ignored) {
            }
        });
        return emitter;
    }

    @GetMapping(path = "/stream/tasks/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskMessages(@PathVariable String taskId) {
        SseEmitter emitter = new SseEmitter();
        repository.findByTaskId(taskId).forEach(message -> {
            try {
                emitter.send(message);
            } catch (Exception ignored) {
            }
        });
        return emitter;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        if (!ALLOWED_ROLES.contains(request.username())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role."));
        }
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    public record DiscussionRequest(String releaseId, String taskId, String parentId, String author, String message) {
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }

    public record ErrorResponse(String message) {
    }
}
