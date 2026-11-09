package com.example.eshop.config;

import com.example.eshop.entity.Product;
import com.example.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cấu hình VectorStore cho Spring AI.
 * Sử dụng LocalSemanticEmbeddingModel nội bộ để tránh lỗi 404 khi kết nối tới Groq
 * (do Groq chỉ hỗ trợ Chat/Vision LLM, không hỗ trợ endpoint text-embedding-ada-002).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class VectorStoreConfig {

    private final ProductRepository productRepository;

    @Bean
    @Primary
    public EmbeddingModel localSemanticEmbeddingModel() {
        return new LocalSemanticEmbeddingModel();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // Cấu hình SimpleVectorStore của Spring AI sử dụng mô hình embedding nội bộ
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
                    vectorStore.add(documents);
                    log.info("Nạp thành công {} sản phẩm vào SimpleVectorStore bằng LocalSemanticEmbeddingModel", documents.size());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi khởi tạo SimpleVectorStore: {}", e.getMessage());
            }
        };
    }

    /**
     * Mô hình nhúng Vector ngữ nghĩa cục bộ chuẩn hóa L2 norm 128 chiều,
     * chạy trực tiếp trong JVM, không cần internet và không phụ thuộc API bên ngoài.
     */
    public static class LocalSemanticEmbeddingModel implements EmbeddingModel {
        private static final int DIMENSIONS = 128;

        @Override
        public float[] embed(Document document) {
            return embed(document != null && document.getText() != null ? document.getText() : "");
        }

        @Override
        public float[] embed(String text) {
            float[] vector = new float[DIMENSIONS];
            if (text == null || text.isBlank()) {
                return vector;
            }

            String cleaned = text.toLowerCase()
                    .replaceAll("[^a-zA-Z0-9àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệđìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ\\s]", " ")
                    .trim();
            String[] tokens = cleaned.split("\\s+");

            for (String t : tokens) {
                if (t.length() < 2) continue;
                int bucket = Math.abs(t.hashCode()) % DIMENSIONS;
                vector[bucket] += 1.0f;
            }

            // Chuẩn hóa vector L2 norm
            double sumSquares = 0.0;
            for (float v : vector) {
                sumSquares += v * v;
            }
            if (sumSquares > 0) {
                float norm = (float) Math.sqrt(sumSquares);
                for (int i = 0; i < DIMENSIONS; i++) {
                    vector[i] /= norm;
                }
            }
            return vector;
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            List<float[]> list = new ArrayList<>(texts.size());
            for (String t : texts) {
                list.add(embed(t));
            }
            return list;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                embeddings.add(new Embedding(embed(instructions.get(i)), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }
    }
}
