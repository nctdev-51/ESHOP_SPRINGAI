package com.example.eshop.ai;

import com.example.eshop.entity.Category;
import com.example.eshop.entity.Product;
import com.example.eshop.repository.CategoryRepository;
import com.example.eshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ProductVectorStoreServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductVectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);

        Category catLaptop = new Category();
        catLaptop.setId(3L);
        catLaptop.setName("Laptop");

        Category catTablet = new Category();
        catTablet.setId(2L);
        catTablet.setName("Máy tính bảng");

        Category catKeyboard = new Category();
        catKeyboard.setId(7L);
        catKeyboard.setName("Bàn phím");

        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("ASUS Vivobook 15 X1504VA");
        p1.setDescription("Laptop văn phòng mỏng nhẹ Core i5 8GB 512GB");
        p1.setPrice(BigDecimal.valueOf(12_490_000));
        p1.setQuantity(20);
        p1.setCategory(catLaptop);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Samsung Galaxy Tab S9 FE");
        p2.setDescription("Máy tính bảng kèm bút S Pen 10.9 inch học tập");
        p2.setPrice(BigDecimal.valueOf(9_490_000));
        p2.setQuantity(18);
        p2.setCategory(catTablet);

        Product p3 = new Product();
        p3.setId(3L);
        p3.setName("Akko 3087 Dragon Ball");
        p3.setDescription("Bàn phím cơ màu đỏ cam switch Akko Jelly Pink");
        p3.setPrice(BigDecimal.valueOf(1_290_000));
        p3.setQuantity(25);
        p3.setCategory(catKeyboard);

        Product p4 = new Product();
        p4.setId(4L);
        p4.setName("ASUS TUF Gaming F15");
        p4.setDescription("Laptop gaming đồ họa RTX 3050 màn hình 144Hz chiến game mượt mà");
        p4.setPrice(BigDecimal.valueOf(18_990_000));
        p4.setQuantity(10);
        p4.setCategory(catLaptop);

        Product p5 = new Product();
        p5.setId(5L);
        p5.setName("Dell XPS 13 Hết Hàng");
        p5.setDescription("Laptop cao cấp siêu mỏng nhẹ");
        p5.setPrice(BigDecimal.valueOf(25_000_000));
        p5.setQuantity(0); // Hết hàng
        p5.setCategory(catLaptop);

        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3, p4, p5));
        when(productRepository.findAllWithCategory()).thenReturn(List.of(p1, p2, p3, p4, p5));

        vectorStoreService = new ProductVectorStoreService(productRepository, categoryRepository);
        vectorStoreService.ingestProductsFromDatabase();
    }

    // Test tìm máy tính với điều kiện giá dưới 10 triệu
    @Test
    void testSemanticSearchComputerUnder10M() {
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("máy tính dưới 10 triệu", 5);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> r.document().price().compareTo(BigDecimal.valueOf(10_000_000)) <= 0));
        assertEquals("Samsung Galaxy Tab S9 FE", results.get(0).document().name());
    }

    // Test tìm kiếm sản phẩm theo màu sắc
    @Test
    void testCriteriaColorRed() {
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("sản phẩm màu đỏ", 5);

        assertFalse(results.isEmpty());
        assertEquals("Akko 3087 Dragon Ball", results.get(0).document().name());
    }

    // Test tìm kiếm ngữ nghĩa laptop gaming
    @Test
    void testSemanticSearchLaptopGaming() {
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("laptop gaming", 5);

        assertFalse(results.isEmpty());
        assertEquals("ASUS TUF Gaming F15", results.get(0).document().name());
    }

    // Test lọc sản phẩm còn hàng
    @Test
    void testFilterInStock() {
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("laptop còn hàng", 10);

        assertFalse(results.isEmpty());
        // Đảm bảo không có sản phẩm nào hết hàng (quantity <= 0) lọt vào kết quả
        assertTrue(results.stream().allMatch(r -> r.document().quantity() > 0));
        assertTrue(results.stream().noneMatch(r -> r.document().name().contains("Hết Hàng")));
    }

    // Test thêm/sửa/xóa sản phẩm động trong VectorStore
    @Test
    void testAddOrUpdateAndRemoveProduct() {
        Category cat = new Category();
        cat.setName("Chuột");

        Product newP = new Product();
        newP.setId(99L);
        newP.setName("Chuột Logitech G102");
        newP.setDescription("Chuột gaming có dây đèn LED RGB");
        newP.setPrice(BigDecimal.valueOf(400_000));
        newP.setQuantity(50);
        newP.setCategory(cat);

        // Thêm sản phẩm
        vectorStoreService.addOrUpdateProduct(newP);
        assertEquals(6, vectorStoreService.getAllDocuments().size());

        // Tìm kiếm sản phẩm mới thêm
        List<ProductVectorStoreService.ScoredProduct> res = vectorStoreService.searchSemantic("chuột gaming giá rẻ", 3);
        assertFalse(res.isEmpty());
        assertEquals("Chuột Logitech G102", res.get(0).document().name());

        // Xóa sản phẩm
        vectorStoreService.removeProduct(99L);
        assertEquals(5, vectorStoreService.getAllDocuments().size());
    }

    // Test gợi ý theo vector sở thích người dùng
    @Test
    void testSearchByUserVector() {
        Map<String, Double> userVector = Map.of("laptop", 0.7, "gaming", 0.7);
        List<ProductVectorStoreService.ScoredProduct> res = vectorStoreService.searchByUserVector(userVector, 3, Set.of(1L), true);

        assertFalse(res.isEmpty());
        // Đã loại trừ sản phẩm #1 và sản phẩm hết hàng #5
        assertTrue(res.stream().noneMatch(r -> r.document().productId().equals(1L)));
        assertTrue(res.stream().noneMatch(r -> r.document().quantity() <= 0));
    }

    // Test công thức tính Cosine Similarity trực tiếp
    @Test
    void testCosineSimilarityMath() {
        Map<String, Double> vecA = Map.of("laptop", 0.6, "gaming", 0.8);
        Map<String, Double> vecB = Map.of("laptop", 0.6, "gaming", 0.8);
        Map<String, Double> vecC = Map.of("ao", 0.7, "quan", 0.7);

        // Hai vector đồng nhất -> Cosine = 1.0 (0.6*0.6 + 0.8*0.8 = 0.36 + 0.64 = 1.0)
        double scoreSame = vectorStoreService.calculateCosineSimilarity(vecA, vecB);
        assertEquals(1.0, scoreSame, 0.001);

        // Hai vector không có từ nào chung -> Cosine = 0.0
        double scoreDifferent = vectorStoreService.calculateCosineSimilarity(vecA, vecC);
        assertEquals(0.0, scoreDifferent, 0.001);
    }

    // Test thống kê sản phẩm
    @Test
    void testProductStatistics() {
        Map<String, Object> stats = vectorStoreService.getProductStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.get("totalProducts"));
        assertEquals(73, stats.get("totalStock"));
    }
}
