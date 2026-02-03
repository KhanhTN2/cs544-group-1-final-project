package com.cs544.aichat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.aichat.service.ChatEventProducer;
import com.cs544.aichat.service.ChatEventProducer.ChatResponse;
import com.cs544.aichat.service.AiChatMetrics;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {
    private final ChatEventProducer eventProducer;
    private final AiChatMetrics metrics;

    public AiChatController(ChatEventProducer eventProducer, AiChatMetrics metrics) {
        this.eventProducer = eventProducer;
        this.metrics = metrics;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        var sample = metrics.startTimer();
        try {
            String reply = "AI reply to: " + request.prompt();
            ChatResponse response = new ChatResponse(request.prompt(), reply);
            eventProducer.publishChatResponse(response);
            return ResponseEntity.ok(response);
        } finally {
            metrics.stopTimer(sample);
        }
    }

    public record ChatRequest(String prompt) {
    }
}
