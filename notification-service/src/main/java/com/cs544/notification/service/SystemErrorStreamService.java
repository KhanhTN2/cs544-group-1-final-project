package com.cs544.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SystemErrorStreamService {
    private SystemErrorEvent lastError;
    private final List<SystemErrorEvent> recentErrors = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SystemErrorEvent getLastError() {
        return lastError;
    }

    public List<SystemErrorEvent> getRecentErrors() {
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

    public void recordError(SystemErrorEvent event) {
        this.lastError = event;
        recentErrors.add(0, event);
        if (recentErrors.size() > 50) {
            recentErrors.remove(recentErrors.size() - 1);
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }
}
