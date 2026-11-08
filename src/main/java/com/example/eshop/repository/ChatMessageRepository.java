package com.example.eshop.repository;

import com.example.eshop.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<ChatMessage> findByUserNameOrderByCreatedAtAsc(String userName);

    void deleteByConversationId(String conversationId);
}
