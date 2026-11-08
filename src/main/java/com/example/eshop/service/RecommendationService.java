package com.example.eshop.service;

import com.example.eshop.ai.ProductVectorStoreService;
import com.example.eshop.entity.UserEvent;
import com.example.eshop.entity.UserPreferenceProfile;
import com.example.eshop.repository.OrderRepository;
import com.example.eshop.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final UserTrackingService userTrackingService;
    private final UserPreferenceVectorService preferenceVectorService;
    private final ProductVectorStoreService vectorStoreService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public record Recommendation(
            Long id,
            String name,
            String category,
            String imageUrl,
            java.math.BigDecimal price,
            int quantity,
            double score
    ) {}

    @Transactional(readOnly = true)
    public List<Recommendation> recommend(String username, String anonymousId, int limit) {
        return recommend(username, anonymousId, null, limit);
    }

    @Transactional(readOnly = true)
    public List<Recommendation> recommend(String username, String anonymousId, Long currentProductId, int limit) {
        Long userId = username == null ? null
                : userRepository.findByUsername(username).map(user -> user.getId()).orElse(null);

        List<UserEvent> events = userTrackingService.recentEvents(userId, anonymousId);
        UserPreferenceProfile profile = preferenceVectorService.buildProfile(events);
        Set<Long> excluded = collectExcludedProductIds(userId, username, events, currentProductId);

        return vectorStoreService.getAllDocuments().stream()
                .filter(document -> document.quantity() > 0)
                .filter(document -> !excluded.contains(document.productId()))
                .map(document -> new Recommendation(
                        document.productId(),
                        document.name(),
                        document.categoryName(),
                        document.imageUrl(),
                        document.price(),
                        document.quantity(),
                        profile.getVector().isEmpty() ? 0.0
                                : vectorStoreService.calculateCosineSimilarity(profile.getVector(), document.vector())
                ))
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparing(Recommendation::name))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private Set<Long> collectExcludedProductIds(Long userId, String username, List<UserEvent> events, Long currentProductId) {
        Set<Long> excluded = new HashSet<>();
        if (currentProductId != null) {
            excluded.add(currentProductId);
        }

        // Chỉ loại trừ các sản phẩm đã mua (purchase), không loại trừ product_view để tôn trọng sở thích người dùng
        for (UserEvent event : events) {
            if (event.getProductId() != null && "purchase".equals(event.getEventType())) {
                excluded.add(event.getProductId());
            }
        }

        if (username != null) {
            orderRepository.findByUserUsernameOrderByOrderDateDesc(username).forEach(order ->
                    order.getDetails().forEach(detail -> {
                        if (detail.getProduct() != null) excluded.add(detail.getProduct().getId());
                    }));
        }
        return excluded;
    }
}
