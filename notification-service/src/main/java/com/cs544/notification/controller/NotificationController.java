package com.cs544.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cs544.notification.service.NotificationEventProducer;
import com.cs544.notification.service.SystemErrorAlert;
import com.cs544.notification.service.SystemErrorEvent;
import com.cs544.notification.service.SystemErrorStreamService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationEventProducer producer;
    private final SystemErrorStreamService streamService;

    public NotificationController(
            NotificationEventProducer producer,
            SystemErrorStreamService streamService
    ) {
        this.producer = producer;
        this.streamService = streamService;
    }

    @PostMapping("/system-error")
    public ResponseEntity<SystemErrorAlert> publishSystemError(@RequestBody SystemErrorEvent request) {
        producer.publishSystemError(request);
        SystemErrorAlert alert = streamService.recordError(request);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/last")
    public ResponseEntity<SystemErrorAlert> getLastErrorEvent() {
        if (streamService.getLastError() == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(streamService.getLastError());
    }

    @GetMapping
    public ResponseEntity<List<SystemErrorAlert>> listAlerts() {
        return ResponseEntity.ok(streamService.getRecentErrors());
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter streamAlerts() {
        return streamService.createEmitter();
    }

    public record ErrorResponse(String message) {
    }
}
