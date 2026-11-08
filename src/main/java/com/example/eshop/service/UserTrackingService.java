package com.example.eshop.service;

import com.example.eshop.entity.User;
import com.example.eshop.entity.UserEvent;
import com.example.eshop.repository.UserEventRepository;
import com.example.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserTrackingService {
    private static final Set<String> EVENT_TYPES = Set.of(
            "page_view",
            "product_view",
            "search",
            "add_to_cart",
            "remove_from_cart",
            "checkout_started",
            "purchase",
            "chat_message",
            "recommendation_click"
    );

    private final UserEventRepository userEventRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserEvent track(String username, Map<String, Object> payload) {
        String eventType = textValue(payload.get("eventType"), 50, "eventType");
        if (eventType == null || !EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Unsupported eventType");
        }

        UserEvent event = new UserEvent();
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username).ifPresent(event::setUser);
        }

        event.setAnonymousId(textValue(payload.get("anonymousId"), 100, "anonymousId"));
        event.setEventType(eventType);
        event.setSessionId(textValue(payload.get("sessionId"), 100, "sessionId"));
        event.setPageUrl(textValue(payload.get("pageUrl"), 500, "pageUrl"));
        event.setProductId(longValue(payload.get("productId")));
        event.setMetadata(textValue(payload.get("metadata"), 5000, "metadata"));
        return userEventRepository.save(event);
    }

    public List<UserEvent> recentEvents(Long userId, String anonymousId) {
        if (userId != null && anonymousId != null) {
            return userEventRepository.findRecentByUserOrAnonymous(userId, anonymousId)
                    .stream().limit(100).toList();
        }
        if (userId != null) return userEventRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId);
        if (anonymousId != null) return userEventRepository.findTop100ByAnonymousIdOrderByCreatedAtDesc(anonymousId);
        return List.of();
    }

    private String textValue(Object value, int maxLength, String fieldName) {
        if (value == null) return null;
        String text = String.valueOf(value);
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return text;
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        try {
            long productId = Long.parseLong(String.valueOf(value));
            if (productId <= 0) throw new IllegalArgumentException("productId must be positive");
            return productId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("productId must be a number", e);
        }
    }
}
