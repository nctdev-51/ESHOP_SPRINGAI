package com.example.eshop.ai;

import com.example.eshop.entity.User;
import com.example.eshop.entity.UserEvent;
import com.example.eshop.entity.UserPreferenceProfile;
import com.example.eshop.repository.UserRepository;
import com.example.eshop.service.RecommendationService;
import com.example.eshop.service.UserPreferenceVectorService;
import com.example.eshop.service.UserTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller phục vụ công cụ trực quan hóa (Visual Demo & Lab) cơ chế vận hành
 * của Vector Database và cầu nối với User Tracking trong eShop.
 */
@Controller
@RequestMapping("/ai/vector-demo")
@RequiredArgsConstructor
@Slf4j
public class VectorDemoController {

    private final ProductVectorStoreService vectorStoreService;
    private final UserTrackingService userTrackingService;
    private final UserPreferenceVectorService userPreferenceVectorService;
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    public String demoPage(Model model, Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName()))
                ? authentication.getName() : null;

        var allDocs = vectorStoreService.getAllDocuments();
        var stats = vectorStoreService.getProductStatistics();

        List<String> sampleQueries = List.of(
                "laptop gaming cấu hình cao màu đỏ dưới 20 triệu",
                "máy tính xách tay văn phòng mỏng nhẹ cho sinh viên",
                "máy tính bảng ipad màn hình cảm ứng học tập",
                "màn hình hiển thị đồ họa 27 inch màu đen còn hàng",
                "bàn phím cơ chuột gaming giá rẻ dưới 2 triệu"
        );

        model.addAttribute("username", username);
        model.addAttribute("totalDocs", allDocs.size());
        model.addAttribute("stats", stats);
        model.addAttribute("sampleQueries", sampleQueries);
        return "ai/vector-demo";
    }

    /**
     * API trả về toàn bộ các bước ETL, Tokenization, Semantic Synonyms, L2 Norm và Cosine Similarity
     */
    @GetMapping("/inspect")
    @ResponseBody
    public ProductVectorStoreService.VectorInspectionResult inspect(
            @RequestParam(name = "query", defaultValue = "laptop gaming dưới 20 triệu") String query) {
        return vectorStoreService.inspectQueryPipeline(query);
    }

    /**
     * API cung cấp tọa độ 2D (x, y) cho Canvas trực quan không gian Vector
     */
    @GetMapping("/canvas-data")
    @ResponseBody
    public Map<String, Object> canvasData(
            @RequestParam(name = "query", defaultValue = "laptop gaming") String query) {
        return vectorStoreService.get2DProjectionData(query);
    }

    /**
     * API khảo sát cách User Tracking sinh ra Hồ sơ Vector Sở thích (User Preference Vector)
     * và tính Cosine Similarity để gợi ý sản phẩm
     */
    @GetMapping("/user-profile")
    @ResponseBody
    public Map<String, Object> inspectUserProfile(
            @RequestParam(name = "anonymousId", required = false) String anonymousId,
            @RequestParam(name = "username", required = false) String username,
            Authentication authentication) {

        String activeUsername = username;
        if (activeUsername == null && authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            activeUsername = authentication.getName();
        }

        Long userId = null;
        if (activeUsername != null) {
            userId = userRepository.findByUsername(activeUsername).map(User::getId).orElse(null);
        }

        List<UserEvent> events = userTrackingService.recentEvents(userId, anonymousId);
        UserPreferenceProfile profile = userPreferenceVectorService.buildProfile(events);

        // Lấy top chiều có trọng số cao nhất trong profile vector
        List<Map.Entry<String, Double>> topTokens = profile.getVector().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        Map<String, Double> formattedTokens = new LinkedHashMap<>();
        for (var entry : topTokens) {
            formattedTokens.put(entry.getKey(), Math.round(entry.getValue() * 10000.0) / 10000.0);
        }

        var recommendations = recommendationService.recommend(activeUsername, anonymousId, 6);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeUsername", activeUsername);
        response.put("anonymousId", anonymousId);
        response.put("totalEvents", events.size());
        response.put("recentEvents", events.stream().limit(10).map(e -> Map.of(
                "eventType", e.getEventType() != null ? e.getEventType() : "",
                "productId", e.getProductId() != null ? e.getProductId() : 0L,
                "createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : ""
        )).collect(Collectors.toList()));
        response.put("userVectorDimensions", formattedTokens);
        response.put("recommendations", recommendations);
        return response;
    }
}

