package com.example.eshop.service;

import com.example.eshop.ai.ProductAssistantService;
import com.example.eshop.ai.ProductTools;
import com.example.eshop.entity.ChatMessage;
import com.example.eshop.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai chính xác theo mục 4.3 của tài liệu hướng dẫn:
 * Service xử lý RAG & Chat History lưu trữ trực tiếp trong CSDL MariaDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductAssistantService productAssistantService;
    private final ProductTools productTools;

    @Transactional
    public String askAi(String conversationId, String username, String userQuery) {
        try {
            // 1. Lưu câu hỏi của người dùng vào MariaDB
            chatMessageRepository.save(new ChatMessage(conversationId, username, "USER", userQuery));

            // 2. Tìm kiếm ngữ nghĩa trong Vector Database theo Cosine Similarity
            String contextProducts = "";
            try {
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(userQuery)
                        .topK(6)
                        .similarityThreshold(0.4)
                        .build();
                List<Document> matchedDocs = vectorStore.similaritySearch(searchRequest);
                if (matchedDocs != null && !matchedDocs.isEmpty()) {
                    contextProducts = matchedDocs.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));
                }
            } catch (Exception ex) {
                log.warn("Tra cứu SimpleVectorStore không khả dụng ({}), sử dụng dữ liệu ngữ nghĩa dự phòng.", ex.getMessage());
            }

            // Nếu VectorStore ngoài chưa có dữ liệu, lấy từ ProductTools semanticSearch
            if (contextProducts.isBlank()) {
                var localDocs = productTools.semanticSearchProducts(userQuery);
                contextProducts = localDocs.stream()
                    .map(d -> String.format("Sản phẩm: %s. Danh mục: %s. Giá: %,.0f VNĐ. Tồn kho: %d. Màu: %s. Mô tả: %s",
                            d.name(), d.categoryName(), d.price(), d.quantity(), d.color(), d.description()))
                    .collect(Collectors.joining("\n---\n"));
            }

            // 3. Xây dựng System Prompt ràng buộc RAG (Anti-hallucination)
            String systemPromptContent = """
                Bạn là trợ lý tư vấn sản phẩm thông minh của siêu thị điện máy eShop.
                
                QUY TẮC BẮT BUỘC (ANTI-HALLUCINATION):
                1. Bạn CHỈ ĐƯỢC PHÉP gợi ý các sản phẩm có trong danh sách ngữ cảnh được cung cấp bên dưới.
                2. Tuyệt đối KHÔNG ĐƯỢC tự bịa ra sản phẩm, giá bán, hoặc thương hiệu không tồn tại trong dữ liệu.
                3. PHÂN TÍCH NGỮ NGHĨA THÔNG MINH:
                   - Nếu khách hỏi "máy tính dưới 10 triệu", hãy kiểm tra xem có laptop giá này không; nếu không có, hãy phân tích gợi ý các sản phẩm tương đương như Máy tính bảng (Tablet), iPad, phụ kiện hoặc laptop giá gần nhất.
                   - Nếu khách hỏi theo tiêu chí (màu sắc, tính năng chống ồn, pin trâu, đồ họa), hãy gom nhóm và phân loại rõ ràng.
                4. Trả lời bằng tiếng Việt lịch sự, gạch đầu dòng rõ ràng: Tên sản phẩm, Giá niêm yết (VNĐ), và lý do phù hợp.

                DỮ LIỆU SẢN PHẨM TRÍCH XUẤT TỪ CƠ SỞ DỮ LIỆU:
                """ + contextProducts;

            // 4. Nạp lại toàn bộ lịch sử trò chuyện từ MariaDB để AI hiểu ngữ cảnh trước đó
            List<ChatMessage> conversationHistory = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            List<Message> messagesList = new ArrayList<>();
            messagesList.add(new SystemMessage(systemPromptContent));

            for (ChatMessage item : conversationHistory) {
                if ("USER".equalsIgnoreCase(item.getMessageType())) {
                    messagesList.add(new UserMessage(item.getContent()));
                } else {
                    messagesList.add(new AssistantMessage(item.getContent()));
                }
            }

            // 5. Gọi LLM sinh câu trả lời
            String aiAnswer;
            try {
                ChatClient chatClient = chatClientBuilder.build();
                aiAnswer = chatClient.prompt(new Prompt(messagesList)).call().content();
            } catch (Exception ex) {
                // Fallback nếu không kết nối được OpenAI ngoài
                log.warn("Không kết nối được LLM ngoài, sử dụng fallback trợ lý thông minh: {}", ex.getMessage());
                aiAnswer = productAssistantService.chat(conversationId, username, userQuery);
                return aiAnswer;
            }

            if (aiAnswer == null || aiAnswer.isBlank()) {
                aiAnswer = productAssistantService.chat(conversationId, username, userQuery);
                return aiAnswer;
            }

            // 6. Lưu câu trả lời của AI vào MariaDB
            chatMessageRepository.save(new ChatMessage(conversationId, username, "ASSISTANT", aiAnswer));

            return aiAnswer;
        } catch (Exception e) {
            log.error("Lỗi askAi: ", e);
            return productAssistantService.chat(conversationId, username, userQuery);
        }
    }

    public List<ChatMessage> getConversationHistory(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
