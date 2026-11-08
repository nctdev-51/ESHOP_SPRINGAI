package com.example.eshop.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history", indexes = {
        @Index(name = "idx_conversation", columnList = "conversation_id"),
        @Index(name = "idx_chat_created_at", columnList = "created_at")
})
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;

    @Column(name = "user_name", length = 50)
    private String userName = "GUEST";

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType; // "USER" hoặc "ASSISTANT"

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ChatMessage(String conversationId, String userName, String messageType, String content) {
        this.conversationId = conversationId;
        this.userName = (userName != null && !userName.isBlank()) ? userName : "GUEST";
        this.messageType = messageType;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserName() {
        return userName != null ? userName : "GUEST";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return getUserName();
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getRole() {
        return messageType;
    }

    public void setRole(String role) {
        this.messageType = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
