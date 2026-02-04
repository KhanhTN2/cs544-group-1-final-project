package com.cs544.aichat.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.cs544.aichat.model.ChatMessage;
import com.cs544.aichat.service.AiChatService;
import com.cs544.aichat.service.ChatHistoryService;
import com.cs544.aichat.service.ChatEventProducer;
import com.cs544.aichat.service.ChatEventProducer.ChatResponse;
import com.cs544.aichat.service.AiChatMetrics;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {
    private final AiChatService aiChatService;
    private final ChatHistoryService chatHistoryService;
    private final ChatEventProducer eventProducer;
    private final AiChatMetrics metrics;

    public AiChatController(
            AiChatService aiChatService,
            ChatHistoryService chatHistoryService,
            ChatEventProducer eventProducer,
            AiChatMetrics metrics
    ) {
        this.aiChatService = aiChatService;
        this.chatHistoryService = chatHistoryService;
        this.eventProducer = eventProducer;
        this.metrics = metrics;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<ApiChatResponse> chat(@RequestBody ChatRequest request, Authentication authentication) {
        var sample = metrics.startTimer();
        try {
            String prompt = request.prompt() == null ? "" : request.prompt().trim();
            String userId = authentication == null ? "anonymous" : authentication.getName();
            AiChatService.ChatResult result = aiChatService.chat(userId, request.conversationId(), prompt);
            String reply = result.reply();
            ChatResponse response = new ChatResponse(request.prompt(), reply);
            eventProducer.publishChatResponse(response);
            return ResponseEntity.ok(new ApiChatResponse(
                    result.conversationId(),
                    request.prompt(),
                    reply,
                    result.format()
            ));
        } finally {
            metrics.stopTimer(sample);
        }
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<StreamingResponseBody> streamChat(
            @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        String userId = authentication == null ? "anonymous" : authentication.getName();
        StreamingResponseBody stream = outputStream -> {
            var sample = metrics.startTimer();
            try {
                AiChatService.ChatResult result = aiChatService.chatStream(userId, request.conversationId(), prompt, chunk -> {
                    try {
                        outputStream.write(chunk.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (java.io.IOException ioEx) {
                        throw new java.io.UncheckedIOException(ioEx);
                    }
                });
                String reply = result.reply();
                eventProducer.publishChatResponse(new ChatResponse(request.prompt(), reply));
            } catch (java.io.UncheckedIOException ignored) {
                // Client closed the stream; avoid routing to /error after response commit.
            } finally {
                metrics.stopTimer(sample);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(stream);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<List<ConversationResponse>> conversations(Authentication authentication) {
        String userId = authentication == null ? "anonymous" : authentication.getName();
        List<ConversationResponse> conversations = chatHistoryService.listConversations(userId, 25).stream()
                .map(item -> new ConversationResponse(item.conversationId(), item.title(), item.updatedAt()))
                .toList();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<List<MessageResponse>> messages(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        String userId = authentication == null ? "anonymous" : authentication.getName();
        List<MessageResponse> messages = chatHistoryService.listMessages(userId, conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
        return ResponseEntity.ok(messages);
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                "USER".equalsIgnoreCase(message.getRole()) ? "user" : "assistant",
                message.getContent(),
                message.getFormat() == null || message.getFormat().isBlank() ? "text" : message.getFormat(),
                message.getCreatedAt()
        );
    }

    public record ChatRequest(String prompt, String conversationId) {
    }

    public record ApiChatResponse(String conversationId, String prompt, String reply, String format) {
    }

    public record ConversationResponse(String conversationId, String title, Instant updatedAt) {
    }

    public record MessageResponse(
            String id,
            String conversationId,
            String role,
            String content,
            String format,
            Instant createdAt
    ) {
    }
}
