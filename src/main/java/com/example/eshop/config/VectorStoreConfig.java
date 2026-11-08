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
        // Cấu hình SimpleVectorStore của Spring AI
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // Nạp dữ liệu sản phẩm từ database vào VectorStore khi khởi động
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
                    // Chuẩn hóa nội dung mô tả sản phẩm để embedding
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
                            "stock", p.getQuantity(),
                            "category", p.getCategory() != null ? p.getCategory().getName() : ""
                        ))
                        .build();
                    documents.add(doc);
                }

                if (!documents.isEmpty()) {
                    try {
                        vectorStore.add(documents);
                    } catch (Exception ex) {
                        log.warn("Không thể kết nối Embedding Model bên ngoài: {}", ex.getMessage());
                    }
                    log.info("Nạp thành công {} sản phẩm vào VectorStore", documents.size());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi khởi tạo VectorStore: {}", e.getMessage());
            }
        };
    }
}
