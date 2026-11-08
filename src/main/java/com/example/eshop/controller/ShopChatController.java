package com.example.eshop.controller;

import com.example.eshop.service.AiAssistantService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * Controller giao diện hội thoại theo mục 4.4 của tài liệu hướng dẫn (/chat)
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ShopChatController {

    private final AiAssistantService aiAssistantService;

    @GetMapping
    public String showChatPage(HttpSession session, Principal principal, Model model) {
        String conversationId = (String) session.getAttribute("AI_CONVERSATION_ID");
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
            session.setAttribute("AI_CONVERSATION_ID", conversationId);
        }

        String username = (principal != null) ? principal.getName() : "GUEST";
        model.addAttribute("messages", aiAssistantService.getConversationHistory(conversationId));
        model.addAttribute("username", username);

        return "shop/chat";
    }

    @PostMapping("/send")
    public String handleSendMessage(
            @RequestParam("query") String query,
            HttpSession session,
            Principal principal) {

        String conversationId = (String) session.getAttribute("AI_CONVERSATION_ID");
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
            session.setAttribute("AI_CONVERSATION_ID", conversationId);
        }

        String username = (principal != null) ? principal.getName() : "GUEST";
        aiAssistantService.askAi(conversationId, username, query.trim());

        return "redirect:/chat";
    }

    @PostMapping("/clear")
    public String handleClearChat(HttpSession session) {
        session.removeAttribute("AI_CONVERSATION_ID");
        return "redirect:/chat";
    }
}
