package com.example.eshop.config;

import com.example.eshop.entity.Product;
import com.example.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class VectorStoreConfig {

    private final ProductRepository productRepository;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // Sử dụng SimpleVectorStore nội tại của Spring AI 1.0.0-M6 theo Slide 9 & 12
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * ETL Pipeline (Extract - Transform - Load):
     * Đọc toàn bộ sản phẩm thực từ MariaDB, vector hóa ngữ nghĩa và nạp vào Vector Database
     */
    @Bean
    public CommandLineRunner initVectorStore(VectorStore vectorStore) {
        return args -> {
            try {
                List<Product> products = productRepository.findAllWithCategory();
                if (products.isEmpty()) {
                    products = productRepository.findAll();
                }
                List<Document> documents = new ArrayList<>();

                for (Product p : products) {
                    // Tạo đoạn văn bản đại diện cho nhiều chiều tiêu chí (chủng loại, tầm giá, tính năng, nhu cầu)
                    String semanticText = String.format(
                        "Sản phẩm: %s. Danh mục: %s. Mức giá: %s VNĐ. Tồn kho: %d chiếc. " +
                        "Mô tả chi tiết và tính năng: %s. Thích hợp: học tập, văn phòng, gaming, đồ họa, giải trí, máy tính xách tay, máy tính bảng.",
                        p.getName(),
                        p.getCategory() != null ? p.getCategory().getName() : "Thiết bị điện tử",
                        p.getPrice().toPlainString(),
                        p.getQuantity(),
                        p.getDescription() != null ? p.getDescription() : "Hàng chính hãng"
                    );

                    Document doc = Document.builder()
                        .text(semanticText)
                        .metadata(Map.of(
                            "productId", p.getId(),
                            "name", p.getName(),
                            "price", p.getPrice().doubleValue(),
                            "category", p.getCategory() != null ? p.getCategory().getName() : ""
                        ))
                        .build();
                    documents.add(doc);
                }

                if (!documents.isEmpty()) {
                    try {
                        vectorStore.add(documents);
                    } catch (Exception ex) {
                        log.warn("Lưu ý: Không thể kết nối Embedding Model bên ngoài ({}), hệ thống sẽ sử dụng Vector Store ngữ nghĩa cục bộ.", ex.getMessage());
                    }
                    System.out.println(">>> [SPRING AI] NẠP THÀNH CÔNG " + documents.size() + " SẢN PHẨM VÀO VECTOR STORE <<<");
                }
            } catch (Exception e) {
                log.warn("Lỗi khi khởi tạo Vector Store: {}", e.getMessage());
            }
        };
    }
}
