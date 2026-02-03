package com.cs544.aichat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.aichat.security.JwtUtil;
import com.cs544.aichat.service.ChatEventProducer;
import com.cs544.aichat.service.ChatEventProducer.ChatResponse;

import java.util.Set;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {
    private final ChatEventProducer eventProducer;
    private final JwtUtil jwtUtil;
    private static final Set<String> ALLOWED_ROLES = Set.of("release-manager", "dev-1", "dev-2");

    public AiChatController(ChatEventProducer eventProducer, JwtUtil jwtUtil) {
        this.eventProducer = eventProducer;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String reply = "AI reply to: " + request.prompt();
        ChatResponse response = new ChatResponse(request.prompt(), reply);
        eventProducer.publishChatResponse(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        if (!ALLOWED_ROLES.contains(request.username())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role."));
        }
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    public record ChatRequest(String prompt) {
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }

    public record ErrorResponse(String message) {
    }
}
