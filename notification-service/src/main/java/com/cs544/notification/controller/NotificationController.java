package com.cs544.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.notification.security.JwtUtil;
import com.cs544.notification.service.NotificationEventProducer;
import com.cs544.notification.service.SystemErrorEvent;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationEventProducer producer;
    private final JwtUtil jwtUtil;
    private SystemErrorEvent lastError;

    public NotificationController(NotificationEventProducer producer, JwtUtil jwtUtil) {
        this.producer = producer;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/system-error")
    public ResponseEntity<SystemErrorEvent> publishSystemError(@RequestBody SystemErrorEvent request) {
        producer.publishSystemError(request);
        this.lastError = request;
        return ResponseEntity.ok(request);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    @KafkaListener(topics = "system.errors", groupId = "notification-service")
    public void consumeSystemError(SystemErrorEvent event) {
        this.lastError = event;
    }

    public SystemErrorEvent getLastError() {
        return lastError;
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }
}
