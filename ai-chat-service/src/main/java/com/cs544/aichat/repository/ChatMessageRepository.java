package com.cs544.aichat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.cs544.aichat.model.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<ChatMessage> findByUserIdAndConversationIdOrderByCreatedAtDesc(String userId, String conversationId, Pageable pageable);
    List<ChatMessage> findByUserIdAndConversationIdOrderByCreatedAtAsc(String userId, String conversationId);
}
