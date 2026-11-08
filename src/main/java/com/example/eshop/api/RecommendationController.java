package com.example.eshop.api;

import com.example.eshop.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping
    public List<RecommendationService.Recommendation> recommend(
            @RequestParam(name = "anonymousId", required = false) String anonymousId,
            @RequestParam(name = "limit", defaultValue = "8") int limit,
            Authentication authentication) {
        String username = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                ? authentication.getName() : null;
        return recommendationService.recommend(username, anonymousId, limit);
    }
}
