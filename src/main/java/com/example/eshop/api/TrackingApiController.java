package com.example.eshop.api;

import com.example.eshop.entity.UserEvent;
import com.example.eshop.service.UserTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingApiController {
    private final UserTrackingService userTrackingService;

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> track(@RequestBody Map<String, Object> payload,
                                                     Authentication authentication) {
        String username = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                ? authentication.getName() : null;
        try {
            UserEvent saved = userTrackingService.track(username, payload);
            return ResponseEntity.ok(Map.of("id", saved.getId(), "eventType", saved.getEventType()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
