package com.cs544.aichat.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cs544.aichat.model.ChatMessage;

@Service
public class AiChatService {
    private final ChatHistoryService chatHistoryService;
    private final OllamaClient ollamaClient;
    private final SystemRagContextService systemRagContextService;
    private final SystemErrorPublisher systemErrorPublisher;
    private final int historyWindow;

    public AiChatService(
            ChatHistoryService chatHistoryService,
            OllamaClient ollamaClient,
            SystemRagContextService systemRagContextService,
            SystemErrorPublisher systemErrorPublisher,
            @Value("${ai.chat.history-window:5}") int historyWindow
    ) {
        this.chatHistoryService = chatHistoryService;
        this.ollamaClient = ollamaClient;
        this.systemRagContextService = systemRagContextService;
        this.systemErrorPublisher = systemErrorPublisher;
        this.historyWindow = historyWindow;
    }

    public ChatResult chat(String userId, String conversationId, String prompt) {
        String normalizedConversationId = chatHistoryService.normalizeConversationId(conversationId);
        if (isMyTaskSummaryPrompt(prompt)) {
            chatHistoryService.saveUserMessage(userId, normalizedConversationId, prompt);
            String reply = systemRagContextService.summarizeUserTasksHtml(userId);
            chatHistoryService.saveAssistantMessage(userId, normalizedConversationId, reply, "html");
            return new ChatResult(normalizedConversationId, reply, "html");
        }

        List<ChatMessage> recentMessages = chatHistoryService.getRecentMessages(userId, normalizedConversationId, historyWindow);
        String systemContext = systemRagContextService.buildSystemContext(prompt, userId);
        String contextualPrompt = buildContextualPrompt(recentMessages, systemContext, prompt);

        chatHistoryService.saveUserMessage(userId, normalizedConversationId, prompt);
        try {
            String reply = ollamaClient.generateReply(contextualPrompt);
            chatHistoryService.saveAssistantMessage(userId, normalizedConversationId, reply, "text");
            return new ChatResult(normalizedConversationId, reply, "text");
        } catch (RuntimeException ex) {
            systemErrorPublisher.publish("Chat request failed for user " + userId + ": " + ex.getMessage());
            throw ex;
        }
    }

    public ChatResult chatStream(String userId, String conversationId, String prompt, Consumer<String> chunkConsumer) {
        String normalizedConversationId = chatHistoryService.normalizeConversationId(conversationId);
        if (isMyTaskSummaryPrompt(prompt)) {
            chatHistoryService.saveUserMessage(userId, normalizedConversationId, prompt);
            String reply = systemRagContextService.summarizeUserTasksHtml(userId);
            chatHistoryService.saveAssistantMessage(userId, normalizedConversationId, reply, "html");
            chunkConsumer.accept(reply);
            return new ChatResult(normalizedConversationId, reply, "html");
        }

        List<ChatMessage> recentMessages = chatHistoryService.getRecentMessages(userId, normalizedConversationId, historyWindow);
        String systemContext = systemRagContextService.buildSystemContext(prompt, userId);
        String contextualPrompt = buildContextualPrompt(recentMessages, systemContext, prompt);

        chatHistoryService.saveUserMessage(userId, normalizedConversationId, prompt);
        try {
            String reply = ollamaClient.generateReplyStream(contextualPrompt, chunkConsumer);
            chatHistoryService.saveAssistantMessage(userId, normalizedConversationId, reply, "text");
            return new ChatResult(normalizedConversationId, reply, "text");
        } catch (RuntimeException ex) {
            systemErrorPublisher.publish("Chat stream failed for user " + userId + ": " + ex.getMessage());
            throw ex;
        }
    }

    private String buildContextualPrompt(List<ChatMessage> recentMessages, String systemContext, String prompt) {
        String historyBlock = recentMessages.isEmpty()
                ? "No prior messages."
                : recentMessages.stream()
                        .map(message -> message.getRole() + ": " + message.getContent())
                        .collect(Collectors.joining("\n"));
        return String.join("\n\n",
                "You are an assistant for software developers. Use ONLY provided system context when stating facts.",
                "If context is missing, say what information is unavailable and what to check.",
                "Do not mention internal database names or storage technologies in the final answer.",
                systemContext,
                "Recent chat history:\n" + historyBlock,
                "Current user question:\n" + prompt
        );
    }

    private boolean isMyTaskSummaryPrompt(String prompt) {
        if (prompt == null) {
            return false;
        }
        String normalized = prompt.toLowerCase();
        return normalized.contains("my task");
    }

    public record ChatResult(String conversationId, String reply, String format) {
    }
}
