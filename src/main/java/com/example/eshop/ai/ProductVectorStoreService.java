package com.example.eshop.ai;

import com.example.eshop.entity.Category;
import com.example.eshop.entity.Product;
import com.example.eshop.repository.CategoryRepository;
import com.example.eshop.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service xử lý tìm kiếm ngữ nghĩa (Semantic Search) và tính Cosine Similarity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVectorStoreService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Danh sách lưu trữ Document và Vector Embedding của sản phẩm trong bộ nhớ
    private final List<ProductDocument> vectorStore = new ArrayList<>();

    // Từ điển ánh xạ từ khóa tương đồng phục vụ tìm kiếm ngữ nghĩa
    private static final Map<String, List<String>> SEMANTIC_SYNONYMS = new HashMap<>();

    static {
        SEMANTIC_SYNONYMS.put("máy tính", List.of("laptop", "máy tính xách tay", "máy tính bảng", "tablet", "ipad", "macbook", "pc", "workstation", "gaming", "văn phòng", "học tập"));
        SEMANTIC_SYNONYMS.put("may tinh", List.of("laptop", "may tinh xach tay", "may tinh bang", "tablet", "ipad", "macbook", "pc", "workstation", "gaming"));
        SEMANTIC_SYNONYMS.put("laptop", List.of("máy tính", "máy tính xách tay", "notebook", "macbook", "vivobook", "inspiron", "thinkpad", "gaming", "văn phòng", "workstation"));
        SEMANTIC_SYNONYMS.put("máy tính bảng", List.of("tablet", "ipad", "tab", "máy tính", "màn hình cảm ứng", "học tập"));
        SEMANTIC_SYNONYMS.put("tablet", List.of("máy tính bảng", "ipad", "galaxy tab", "xiaomi pad", "máy tính"));
        SEMANTIC_SYNONYMS.put("điện thoại", List.of("smartphone", "phone", "iphone", "galaxy", "redmi", "reno", "di động"));
        SEMANTIC_SYNONYMS.put("gaming", List.of("game", "game thủ", "chơi game", "cấu hình cao", "đồ hoạ", "144hz", "165hz"));
        SEMANTIC_SYNONYMS.put("văn phòng", List.of("học tập", "công sở", "nhẹ", "mỏng nhẹ", "pin trâu", "làm việc"));
        SEMANTIC_SYNONYMS.put("workstation", List.of("đồ hoạ", "render", "chuyên nghiệp", "thinkpad", "proart", "hiệu năng cao"));
        SEMANTIC_SYNONYMS.put("tai nghe", List.of("earphone", "headphone", "airpods", "chống ồn", "âm thanh"));
        SEMANTIC_SYNONYMS.put("bàn phím", List.of("keyboard", "bàn phím cơ", "switch", "akko", "keychron", "logitech"));
        SEMANTIC_SYNONYMS.put("chuột", List.of("mouse", "chuột không dây", "chuột gaming", "logitech", "razer"));
        SEMANTIC_SYNONYMS.put("màn hình", List.of("monitor", "display", "24 inch", "27 inch", "ips", "ultrasharp", "proart"));
    }

    public record ProductDocument(
            Long productId,
            String name,
            String categoryName,
            BigDecimal price,
            int quantity,
            String imageUrl,
            String description,
            String color,
            String fullSemanticText,
            Map<String, Double> vector
    ) {}

    public record ScoredProduct(
            ProductDocument document,
            double similarityScore
    ) {}

    // Load dữ liệu sản phẩm từ database vào bộ nhớ khi khởi động
    @PostConstruct
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public synchronized void ingestProductsFromDatabase() {
        vectorStore.clear();
        List<Product> products = productRepository.findAllWithCategory();
        if (products.isEmpty()) {
            products = productRepository.findAll();
        }
        log.info("Nạp dữ liệu từ MariaDB vào VectorStore. Tổng sản phẩm: {}", products.size());

        for (Product p : products) {
            String catName = p.getCategory() != null ? p.getCategory().getName() : "Khác";
            String detectedColor = detectColor(p.getName() + " " + p.getDescription());
            String fullSemanticText = buildSemanticText(p, catName, detectedColor);

            // Tạo vector embedding cho sản phẩm
            Map<String, Double> vector = computeEmbeddingVector(fullSemanticText);

            ProductDocument doc = new ProductDocument(
                    p.getId(),
                    p.getName(),
                    catName,
                    p.getPrice(),
                    p.getQuantity(),
                    p.getImageUrl(),
                    p.getDescription(),
                    detectedColor,
                    fullSemanticText,
                    vector
            );
            vectorStore.add(doc);
        }

        log.info("Hoàn tất nạp {} tài liệu vào VectorStore", vectorStore.size());
    }

    // Nhận diện màu sắc từ mô tả hoặc tên sản phẩm
    private String detectColor(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("đỏ") || lower.contains("red") || lower.contains("dragon ball")) return "Đỏ";
        if (lower.contains("đen") || lower.contains("black") || lower.contains("dark")) return "Đen";
        if (lower.contains("trắng") || lower.contains("white")) return "Trắng";
        if (lower.contains("bạc") || lower.contains("silver")) return "Bạc";
        if (lower.contains("xám") || lower.contains("grey") || lower.contains("gray")) return "Xám";
        if (lower.contains("xanh") || lower.contains("blue") || lower.contains("navy")) return "Xanh";
        if (lower.contains("vàng") || lower.contains("gold") || lower.contains("yellow")) return "Vàng";
        if (lower.contains("hồng") || lower.contains("pink")) return "Hồng";
        return "Tiêu chuẩn";
    }

    // Xây dựng đoạn văn bản đại diện cho sản phẩm để embedding
    private String buildSemanticText(Product p, String catName, String color) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName()).append(" ");
        sb.append("Danh mục: ").append(catName).append(" ");
        if (p.getDescription() != null) {
            sb.append(p.getDescription()).append(" ");
        }
        sb.append("Màu sắc: ").append(color).append(" ");

        // Gắn thêm từ khóa phân loại thiết bị
        String lowerCat = catName.toLowerCase();
        String lowerName = p.getName().toLowerCase();
        String lowerDesc = (p.getDescription() != null ? p.getDescription() : "").toLowerCase();

        if (lowerCat.contains("laptop")) {
            sb.append("máy tính máy tính xách tay notebook ");
            if (lowerName.contains("gaming") || lowerDesc.contains("gaming") || lowerDesc.contains("rtx")) {
                sb.append("laptop gaming máy tính chơi game đồ hoạ ");
            } else if (lowerName.contains("thinkpad") || lowerName.contains("macbook")) {
                sb.append("laptop doanh nhân laptop cao cấp workstation làm việc ");
            } else {
                sb.append("laptop văn phòng laptop học tập sinh viên ");
            }
        } else if (lowerCat.contains("máy tính bảng")) {
            sb.append("máy tính máy tính bảng tablet ipad thiết bị học tập di động ");
        } else if (lowerCat.contains("màn hình")) {
            sb.append("màn hình máy tính monitor hiển thị gaming đồ hoạ ");
        } else if (lowerCat.contains("chuột") || lowerCat.contains("bàn phím")) {
            sb.append("phụ kiện máy tính gaming văn phòng ");
        }

        // Gắn nhãn phân khúc giá
        double priceVal = p.getPrice().doubleValue();
        if (priceVal < 5_000_000) {
            sb.append("giá rẻ dưới 5 triệu bình dân ");
        } else if (priceVal <= 10_000_000) {
            sb.append("dưới 10 triệu tầm trung giá tốt sinh viên ");
        } else if (priceVal <= 20_000_000) {
            sb.append("từ 10 đến 20 triệu cận cao cấp ");
        } else {
            sb.append("trên 20 triệu cao cấp flagship chuyên nghiệp ");
        }

        return sb.toString();
    }

    // Tính toán vector biểu diễn cho đoạn văn bản
    public Map<String, Double> computeEmbeddingVector(String text) {
        Map<String, Double> vector = new HashMap<>();
        if (text == null || text.isBlank()) return vector;

        String cleaned = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệđìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ\\s]", " ")
                .trim();
        String[] tokens = cleaned.split("\\s+");

        // Đếm tần suất xuất hiện của từng từ (TF)
        for (String t : tokens) {
            if (t.length() < 2) continue;
            vector.put(t, vector.getOrDefault(t, 0.0) + 1.0);
        }

        // Thêm trọng số cho các từ liên quan trong từ điển
        for (Map.Entry<String, List<String>> entry : SEMANTIC_SYNONYMS.entrySet()) {
            if (cleaned.contains(entry.getKey())) {
                for (String syn : entry.getValue()) {
                    vector.put(syn, vector.getOrDefault(syn, 0.0) + 0.6);
                }
            }
        }

        // Chuẩn hóa vector (L2 norm)
        double norm = 0.0;
        for (double v : vector.values()) {
            norm += v * v;
        }
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (Map.Entry<String, Double> e : vector.entrySet()) {
                e.setValue(e.getValue() / norm);
            }
        }
        return vector;
    }

    // Tính Cosine Similarity giữa 2 vector
    public double calculateCosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            Double valB = vecB.get(entry.getKey());
            if (valB != null) {
                dotProduct += entry.getValue() * valB;
            }
        }
        // Do đã chuẩn hóa L2 nên tích vô hướng chính là Cosine Similarity
        return Math.min(1.0, Math.max(0.0, dotProduct));
    }

    // Tìm kiếm sản phẩm theo ngữ nghĩa kết hợp lọc theo giá, màu sắc, tồn kho
    public List<ScoredProduct> searchSemantic(String userQuery, int topK) {
        if (vectorStore.isEmpty()) {
            ingestProductsFromDatabase();
        }

        String lowerQuery = userQuery.toLowerCase();

        // 1. Phân tích tiêu chí giá từ câu hỏi
        BigDecimal maxPrice = extractMaxPrice(lowerQuery);
        BigDecimal minPrice = extractMinPrice(lowerQuery);

        // 2. Phân tích tiêu chí màu sắc
        String queryColor = extractQueryColor(lowerQuery);

        // 3. Phân tích tiêu chí tình trạng tồn kho
        boolean requireInStock = extractInStockRequirement(lowerQuery);

        // 4. Vector hoá câu hỏi của người dùng
        Map<String, Double> queryVector = computeEmbeddingVector(userQuery);

        List<ScoredProduct> scoredList = new ArrayList<>();

        for (ProductDocument doc : vectorStore) {
            // Lọc cứng theo tiêu chí tồn kho nếu yêu cầu còn hàng
            if (requireInStock && doc.quantity() <= 0) {
                continue;
            }

            // Lọc cứng theo tiêu chí giá nếu người dùng có yêu cầu
            if (maxPrice != null && doc.price().compareTo(maxPrice) > 0) {
                continue;
            }
            if (minPrice != null && doc.price().compareTo(minPrice) < 0) {
                continue;
            }

            // Tính độ tương đồng Cosine
            double cosine = calculateCosineSimilarity(queryVector, doc.vector());

            // Thưởng điểm nếu sản phẩm khớp màu sắc yêu cầu
            if (queryColor != null && !queryColor.isEmpty()) {
                if (doc.color().equalsIgnoreCase(queryColor) || doc.fullSemanticText().toLowerCase().contains(queryColor.toLowerCase())) {
                    cosine += 0.35; // Ưu tiên sản phẩm đúng màu
                }
            }

            // Thưởng điểm nếu câu hỏi tìm máy tính khớp danh mục máy tính hoặc tablet
            if (lowerQuery.contains("máy tính") || lowerQuery.contains("may tinh")) {
                String cat = doc.categoryName().toLowerCase();
                if (cat.contains("laptop") || cat.contains("máy tính bảng") || cat.contains("màn hình")) {
                    cosine += 0.15; // Ưu tiên thiết bị phù hợp
                }
            }

            // Thưởng điểm chuyên biệt cho truy vấn "laptop gaming" hoặc "gaming"
            if (lowerQuery.contains("gaming") || lowerQuery.contains("chơi game") || lowerQuery.contains("choi game")) {
                String nameLower = doc.name().toLowerCase();
                String descLower = (doc.description() != null ? doc.description() : "").toLowerCase();
                String textLower = doc.fullSemanticText().toLowerCase();
                if (nameLower.contains("gaming") || descLower.contains("gaming") || textLower.contains("rtx") || textLower.contains("144hz") || textLower.contains("165hz")) {
                    cosine += 0.25; // Ưu tiên dòng máy gaming chuyên dụng
                }
            }

            // Giới hạn trong khoảng [0.0, 1.0]
            cosine = Math.min(1.0, Math.max(0.0, cosine));

            if (cosine > 0.05) {
                scoredList.add(new ScoredProduct(doc, cosine));
            }
        }

        // Sắp xếp theo độ tương đồng giảm dần
        scoredList.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));

        return scoredList.stream().limit(topK).collect(Collectors.toList());
    }

    // Trích xuất yêu cầu còn hàng từ câu hỏi
    private boolean extractInStockRequirement(String text) {
        return text.contains("còn hàng") || text.contains("con hang")
                || text.contains("sẵn hàng") || text.contains("san hang")
                || text.contains("có sẵn") || text.contains("co san");
    }

    // Trích xuất giá tối đa từ câu hỏi (ví dụ: "dưới 10 triệu", "< 15tr")
    private BigDecimal extractMaxPrice(String text) {
        Pattern p1 = Pattern.compile("(?:dưới|<|nho hon|nhỏ hơn|tối đa|khong qua|không quá|đến|tới|-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|nghìn|k|dong|đ|vnd)?");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            double num = Double.parseDouble(m1.group(1).replace(",", "."));
            String unit = m1.group(2) != null ? m1.group(2).toLowerCase() : "";
            if (unit.contains("triệu") || unit.contains("tr") || unit.contains("m")) {
                return BigDecimal.valueOf((long) (num * 1_000_000));
            } else if (unit.contains("nghìn") || unit.contains("k")) {
                return BigDecimal.valueOf((long) (num * 1_000));
            } else if (num < 1000) {
                return BigDecimal.valueOf((long) (num * 1_000_000));
            } else {
                return BigDecimal.valueOf((long) num);
            }
        }
        return null;
    }

    // Trích xuất giá tối thiểu từ câu hỏi (ví dụ: "trên 5 triệu", "> 10tr")
    private BigDecimal extractMinPrice(String text) {
        Pattern p = Pattern.compile("(?:trên|>|lon hon|lớn hơn|tối thiểu|tu|từ)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|nghìn|k)?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            double num = Double.parseDouble(m.group(1).replace(",", "."));
            String unit = m.group(2) != null ? m.group(2).toLowerCase() : "";
            if (unit.contains("triệu") || unit.contains("tr") || unit.contains("m")) {
                return BigDecimal.valueOf((long) (num * 1_000_000));
            } else if (unit.contains("nghìn") || unit.contains("k")) {
                return BigDecimal.valueOf((long) (num * 1_000));
            } else if (num < 1000) {
                return BigDecimal.valueOf((long) (num * 1_000_000));
            }
        }
        return null;
    }

    // Trích xuất tiêu chí màu sắc từ câu hỏi
    private String extractQueryColor(String text) {
        if (text.contains("màu đỏ") || text.contains("mau do") || text.contains("màu red")) return "Đỏ";
        if (text.contains("màu đen") || text.contains("mau den") || text.contains("màu black")) return "Đen";
        if (text.contains("màu trắng") || text.contains("mau trang") || text.contains("màu white")) return "Trắng";
        if (text.contains("màu bạc") || text.contains("mau bac") || text.contains("màu silver")) return "Bạc";
        if (text.contains("màu xanh") || text.contains("mau xanh")) return "Xanh";
        if (text.contains("màu vàng") || text.contains("mau vang")) return "Vàng";
        if (text.contains("màu hồng") || text.contains("mau hong")) return "Hồng";
        return null;
    }

    // Cập nhật hoặc thêm mới 1 sản phẩm vào VectorStore khi Admin tạo/sửa
    public synchronized void addOrUpdateProduct(Product product) {
        if (product == null || product.getId() == null) return;
        vectorStore.removeIf(d -> d.productId().equals(product.getId()));

        String catName = product.getCategory() != null ? product.getCategory().getName() : "Khác";
        String detectedColor = detectColor(product.getName() + " " + (product.getDescription() != null ? product.getDescription() : ""));
        String fullSemanticText = buildSemanticText(product, catName, detectedColor);
        Map<String, Double> vector = computeEmbeddingVector(fullSemanticText);

        ProductDocument doc = new ProductDocument(
                product.getId(),
                product.getName(),
                catName,
                product.getPrice(),
                product.getQuantity(),
                product.getImageUrl(),
                product.getDescription(),
                detectedColor,
                fullSemanticText,
                vector
        );
        vectorStore.add(doc);
        log.info("Đã cập nhật VectorStore cho sản phẩm #{} - {}", product.getId(), product.getName());
    }

    // Xóa sản phẩm khỏi VectorStore khi Admin xóa sản phẩm trong CSDL
    public synchronized void removeProduct(Long productId) {
        if (productId == null) return;
        boolean removed = vectorStore.removeIf(d -> d.productId().equals(productId));
        if (removed) {
            log.info("Đã xóa sản phẩm #{} khỏi VectorStore", productId);
        }
    }

    // Tìm kiếm sản phẩm tương đồng dựa trên User Preference Vector (cho module gợi ý)
    public List<ScoredProduct> searchByUserVector(Map<String, Double> userVector, int topK, Set<Long> excludeProductIds, boolean inStockOnly) {
        if (vectorStore.isEmpty()) {
            ingestProductsFromDatabase();
        }
        if (userVector == null || userVector.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredProduct> scoredList = new ArrayList<>();
        for (ProductDocument doc : vectorStore) {
            if (excludeProductIds != null && excludeProductIds.contains(doc.productId())) {
                continue;
            }
            if (inStockOnly && doc.quantity() <= 0) {
                continue;
            }
            double cosine = calculateCosineSimilarity(userVector, doc.vector());
            if (cosine > 0.05) {
                scoredList.add(new ScoredProduct(doc, cosine));
            }
        }

        scoredList.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
        return scoredList.stream().limit(topK).collect(Collectors.toList());
    }

    // Thống kê sản phẩm theo phân khúc giá, danh mục và tồn kho
    public Map<String, Object> getProductStatistics() {
        if (vectorStore.isEmpty()) {
            ingestProductsFromDatabase();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProducts", vectorStore.size());

        // Phân khúc giá
        long under5m = vectorStore.stream().filter(d -> d.price().compareTo(BigDecimal.valueOf(5_000_000)) < 0).count();
        long from5mTo10m = vectorStore.stream().filter(d -> d.price().compareTo(BigDecimal.valueOf(5_000_000)) >= 0 && d.price().compareTo(BigDecimal.valueOf(10_000_000)) <= 0).count();
        long from10mTo20m = vectorStore.stream().filter(d -> d.price().compareTo(BigDecimal.valueOf(10_000_000)) > 0 && d.price().compareTo(BigDecimal.valueOf(20_000_000)) <= 0).count();
        long over20m = vectorStore.stream().filter(d -> d.price().compareTo(BigDecimal.valueOf(20_000_000)) > 0).count();

        Map<String, Long> priceRanges = new LinkedHashMap<>();
        priceRanges.put("Dưới 5 triệu", under5m);
        priceRanges.put("Từ 5 triệu - 10 triệu", from5mTo10m);
        priceRanges.put("Từ 10 triệu - 20 triệu", from10mTo20m);
        priceRanges.put("Trên 20 triệu", over20m);
        stats.put("priceDistribution", priceRanges);

        // Theo danh mục
        Map<String, Long> categoryCount = vectorStore.stream()
                .collect(Collectors.groupingBy(ProductDocument::categoryName, LinkedHashMap::new, Collectors.counting()));
        stats.put("categoryCounts", categoryCount);

        // Tồn kho
        int totalStock = vectorStore.stream().mapToInt(ProductDocument::quantity).sum();
        long lowStockCount = vectorStore.stream().filter(d -> d.quantity() <= 10).count();
        stats.put("totalStock", totalStock);
        stats.put("lowStockCount", lowStockCount);

        return stats;
    }

    public List<ProductDocument> getAllDocuments() {
        if (vectorStore.isEmpty()) {
            ingestProductsFromDatabase();
        }
        return Collections.unmodifiableList(vectorStore);
    }
}
