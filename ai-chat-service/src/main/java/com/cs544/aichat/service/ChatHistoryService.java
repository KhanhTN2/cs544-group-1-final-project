package com.cs544.aichat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.cs544.aichat.model.ChatMessage;
import com.cs544.aichat.repository.ChatMessageRepository;

@Service
public class ChatHistoryService {
    private final ChatMessageRepository repository;

    public ChatHistoryService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public List<ChatMessage> getRecentMessages(String userId, String conversationId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 5));
        List<ChatMessage> recent = repository.findByUserIdAndConversationIdOrderByCreatedAtDesc(
                userId,
                normalizeConversationId(conversationId),
                PageRequest.of(0, safeLimit)
        );
        List<ChatMessage> ordered = new ArrayList<>(recent);
        Collections.reverse(ordered);
        return ordered;
    }

    public void saveUserMessage(String userId, String conversationId, String message) {
        repository.save(new ChatMessage(
                userId,
                normalizeConversationId(conversationId),
                "USER",
                message,
                "text",
                Instant.now()
        ));
    }

    public void saveAssistantMessage(String userId, String conversationId, String message, String format) {
        repository.save(new ChatMessage(
                userId,
                normalizeConversationId(conversationId),
                "ASSISTANT",
                message,
                format == null || format.isBlank() ? "text" : format,
                Instant.now()
        ));
    }

    public List<ConversationSummary> listConversations(String userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 30));
        List<ChatMessage> recent = repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        Map<String, ConversationSummary> byConversation = new LinkedHashMap<>();
        for (ChatMessage message : recent) {
            String conversationId = normalizeConversationId(message.getConversationId());
            if (!byConversation.containsKey(conversationId)) {
                byConversation.put(conversationId, new ConversationSummary(
                        conversationId,
                        "New chat",
                        message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt()
                ));
            }
        }

        List<ConversationSummary> summaries = byConversation.values().stream()
                .map(summary -> {
                    List<ChatMessage> messages = repository.findByUserIdAndConversationIdOrderByCreatedAtAsc(
                            userId,
                            summary.conversationId()
                    );
                    String title = messages.stream()
                            .filter(message -> "USER".equalsIgnoreCase(message.getRole()))
                            .map(ChatMessage::getContent)
                            .map(this::titleFromPrompt)
                            .filter(text -> !text.isBlank())
                            .findFirst()
                            .orElse("New chat");
                    return new ConversationSummary(summary.conversationId(), title, summary.updatedAt());
                })
                .sorted(Comparator.comparing(ConversationSummary::updatedAt).reversed())
                .limit(safeLimit)
                .toList();
        return summaries;
    }

    public List<ChatMessage> listMessages(String userId, String conversationId) {
        return repository.findByUserIdAndConversationIdOrderByCreatedAtAsc(
                userId,
                normalizeConversationId(conversationId)
        );
    }

    public String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "default";
        }
        return conversationId.trim();
    }

    private String titleFromPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        String clean = prompt.replaceAll("\\s+", " ").trim();
        if (clean.isBlank()) {
            return "";
        }
        return clean.length() <= 36 ? clean : clean.substring(0, 36) + "...";
    }

    public record ConversationSummary(String conversationId, String title, Instant updatedAt) {
    }
}
