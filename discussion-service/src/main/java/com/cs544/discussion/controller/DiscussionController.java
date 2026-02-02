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

    public DiscussionController(DiscussionRepository repository, DiscussionEventProducer eventProducer, JwtUtil jwtUtil) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<DiscussionMessage> createMessage(@RequestBody DiscussionRequest request) {
        DiscussionMessage message = repository.save(new DiscussionMessage(request.releaseId(), request.author(), request.message()));
        eventProducer.publishMessageCreated(message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{releaseId}")
    public ResponseEntity<List<DiscussionMessage>> listMessages(@PathVariable String releaseId) {
        return ResponseEntity.ok(repository.findByReleaseId(releaseId));
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

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    public record DiscussionRequest(String releaseId, String author, String message) {
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }
}
