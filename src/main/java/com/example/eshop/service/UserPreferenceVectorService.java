package com.example.eshop.service;

import com.example.eshop.ai.ProductVectorStoreService;
import com.example.eshop.entity.UserEvent;
import com.example.eshop.entity.UserPreferenceProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserPreferenceVectorService {
    private final ProductVectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper;

    public UserPreferenceProfile buildProfile(List<UserEvent> events) {
        UserPreferenceProfile profile = new UserPreferenceProfile();
        for (UserEvent event : events) {
            double weight = switch (event.getEventType() == null ? "" : event.getEventType()) {
                case "purchase" -> 8.0;
                case "add_to_cart" -> 5.0;
                case "product_view" -> 3.0;
                case "recommendation_click" -> 4.0;
                case "search", "chat_message" -> 2.0;
                default -> 1.0;
            };

            Map<String, Double> eventVector = null;
            if (event.getProductId() != null) {
                eventVector = vectorStoreService.getAllDocuments().stream()
                        .filter(document -> document.productId().equals(event.getProductId()))
                        .map(ProductVectorStoreService.ProductDocument::vector)
                        .findFirst().orElse(null);
            } else if ("search".equals(event.getEventType()) || "chat_message".equals(event.getEventType())) {
                String query = readMetadataValue(event.getMetadata(), "query");
                if (query != null) {
                    eventVector = vectorStoreService.computeEmbeddingVector(query);
                }
            }
            profile.add(eventVector, weight);
        }
        profile.normalize();
        return profile;
    }

    private String readMetadataValue(String metadata, String field) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(metadata);
            JsonNode value = node.get(field);
            return value == null ? null : value.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
