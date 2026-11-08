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

@Service
@RequiredArgsConstructor
public class UserTrackingService {
    private final UserEventRepository userEventRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserEvent track(String username, Map<String, Object> payload) {
        UserEvent event = new UserEvent();
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username).ifPresent(event::setUser);
        }

        event.setAnonymousId(stringValue(payload.get("anonymousId")));
        event.setEventType(stringValue(payload.get("eventType")));
        event.setSessionId(stringValue(payload.get("sessionId")));
        event.setPageUrl(stringValue(payload.get("pageUrl")));
        event.setProductId(longValue(payload.get("productId")));
        event.setMetadata(stringValue(payload.get("metadata")));
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
