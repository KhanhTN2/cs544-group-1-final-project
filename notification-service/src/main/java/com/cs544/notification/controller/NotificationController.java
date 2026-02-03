package com.cs544.notification.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cs544.notification.security.JwtUtil;
import com.cs544.notification.service.NotificationEventProducer;
import com.cs544.notification.service.SystemErrorEvent;
import com.cs544.notification.service.SystemErrorStreamService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationEventProducer producer;
    private final JwtUtil jwtUtil;
    private final SystemErrorStreamService streamService;
    private static final Set<String> ALLOWED_ROLES = Set.of("release-manager", "dev-1", "dev-2");

    public NotificationController(
            NotificationEventProducer producer,
            JwtUtil jwtUtil,
            SystemErrorStreamService streamService
    ) {
        this.producer = producer;
        this.jwtUtil = jwtUtil;
        this.streamService = streamService;
    }

    @PostMapping("/system-error")
    public ResponseEntity<SystemErrorEvent> publishSystemError(@RequestBody SystemErrorEvent request) {
        producer.publishSystemError(request);
        streamService.recordError(request);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        if (!ALLOWED_ROLES.contains(request.username())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role."));
        }
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    @GetMapping("/last")
    public ResponseEntity<SystemErrorEvent> getLastErrorEvent() {
        if (streamService.getLastError() == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(streamService.getLastError());
    }

    @GetMapping
    public ResponseEntity<List<SystemErrorEvent>> listAlerts() {
        return ResponseEntity.ok(streamService.getRecentErrors());
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter streamAlerts() {
        return streamService.createEmitter();
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }

    public record ErrorResponse(String message) {
    }
}
