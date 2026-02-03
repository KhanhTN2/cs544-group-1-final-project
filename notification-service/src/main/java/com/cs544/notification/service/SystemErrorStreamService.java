package com.cs544.notification.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SystemErrorStreamService {
    private SystemErrorAlert lastError;
    private final List<SystemErrorAlert> recentErrors = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SystemErrorAlert getLastError() {
        return lastError;
    }

    public List<SystemErrorAlert> getRecentErrors() {
        return recentErrors;
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
        return emitter;
    }

    public SystemErrorAlert recordError(SystemErrorEvent event) {
        return recordError(UUID.randomUUID().toString(), event);
    }

    public SystemErrorAlert recordError(String id, SystemErrorEvent event) {
        SystemErrorAlert alert = new SystemErrorAlert(
                id,
                event.service(),
                event.message(),
                Instant.now()
        );
        this.lastError = alert;
        recentErrors.add(0, alert);
        if (recentErrors.size() > 50) {
            recentErrors.remove(recentErrors.size() - 1);
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(alert);
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
        return alert;
    }
}
