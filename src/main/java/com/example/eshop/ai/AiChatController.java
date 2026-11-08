package com.example.eshop.ai;

import com.example.eshop.entity.ChatMessage;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final ProductAssistantService assistantService;
    private final ChatHistoryService chatHistoryService;
    private final ProductVectorStoreService vectorStoreService;

    @GetMapping("/chat")
    public String chatPage(HttpSession session, Authentication authentication, Model model) {
        String username = getUsername(authentication);
        String conversationId = session.getId();
        List<ChatMessage> history = chatHistoryService.getHistory(conversationId);

        model.addAttribute("username", username);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("history", history);
        model.addAttribute("vectorCount", vectorStoreService.getAllDocuments().size());
        return "ai/chat";
    }

    /**
     * Nhận tin nhắn đa phương thức (Hình ảnh + Văn bản) để phân tích bằng Vision AI & Vector DB
     */
    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, Object> askMultipart(
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session,
            Authentication authentication) {

        String username = getUsername(authentication);
        String conversationId = session.getId();

        byte[] imageBytes = null;
        String imageMimeType = null;
        String originalFilename = null;
        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            try {
                imageBytes = image.getBytes();
                imageMimeType = image.getContentType();
                originalFilename = StringUtils.cleanPath(image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg");

                // Lưu ảnh vào thư mục uploads/ai/ để phục vụ hiển thị
                Path uploadDir = Paths.get("uploads/ai");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                Path filePath = uploadDir.resolve(savedFilename);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imageUrl = "/uploads/ai/" + savedFilename;

            } catch (IOException e) {
                log.error("Lỗi khi lưu tệp ảnh tải lên: ", e);
            }
        }

        if ((message == null || message.trim().isEmpty()) && (imageBytes == null || imageBytes.length == 0)) {
            return Map.of("error", "Vui lòng nhập câu hỏi hoặc chọn một hình ảnh.");
        }

        String reply = assistantService.chat(conversationId, username, message, imageBytes, imageMimeType, originalFilename, imageUrl);

        return Map.of(
                "reply", reply,
                "conversationId", conversationId,
                "username", username,
                "imageUrl", imageUrl != null ? imageUrl : ""
        );
    }

    /**
     * Nhận tin nhắn JSON thuần văn bản (tương thích ngược)
     */
    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> askJson(
            @RequestBody Map<String, String> request,
            HttpSession session,
            Authentication authentication) {

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Map.of("error", "Nội dung tin nhắn không được để trống");
        }

        String username = getUsername(authentication);
        String conversationId = session.getId();

        String reply = assistantService.chat(conversationId, username, message.trim());
        return Map.of(
                "reply", reply,
                "conversationId", conversationId,
                "username", username
        );
    }

    @GetMapping("/history")
    @ResponseBody
    public List<ChatMessage> getHistory(HttpSession session) {
        return chatHistoryService.getHistory(session.getId());
    }

    @PostMapping("/clear-history")
    @ResponseBody
    public Map<String, Object> clearHistory(HttpSession session) {
        chatHistoryService.clearHistory(session.getId());
        return Map.of("success", true, "message", "Đã xóa toàn bộ lịch sử trò chuyện trong CSDL.");
    }

    @GetMapping("/vector-stats")
    @ResponseBody
    public Map<String, Object> getVectorStats() {
        return vectorStoreService.getProductStatistics();
    }

    private String getUsername(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "Khách vãng lai";
    }
}
