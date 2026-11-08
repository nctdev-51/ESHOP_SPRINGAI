package com.example.eshop.ai;

import com.example.eshop.entity.ChatMessage;
import com.example.eshop.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage saveMessage(String conversationId, String username, String role, String content) {
        ChatMessage msg = new ChatMessage(conversationId, username, role, content);
        ChatMessage saved = chatMessageRepository.save(msg);
        log.debug("Đã lưu tin nhắn vào CSDL: [{}] {} - {}", role, conversationId, content);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public void clearHistory(String conversationId) {
        chatMessageRepository.deleteByConversationId(conversationId);
        log.info("Đã xóa toàn bộ lịch sử trò chuyện trong CSDL cho conversationId: {}", conversationId);
    }
}
