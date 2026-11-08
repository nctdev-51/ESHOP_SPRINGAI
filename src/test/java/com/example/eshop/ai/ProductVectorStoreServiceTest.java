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

        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));
        when(productRepository.findAllWithCategory()).thenReturn(List.of(p1, p2, p3));

        vectorStoreService = new ProductVectorStoreService(productRepository, categoryRepository);
        vectorStoreService.ingestProductsFromDatabase();
    }

    @Test
    void testSemanticSearchComputerUnder10M() {
        // Hỏi "máy tính dưới 10 triệu":
        // p1 (12.49M) bị lọc vì > 10M
        // p2 (Samsung Tab S9 FE 9.49M) là máy tính bảng, dưới 10M -> khớp ngữ nghĩa máy tính!
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("máy tính dưới 10 triệu", 5);

        assertFalse(results.isEmpty(), "Phải tìm thấy sản phẩm máy tính dưới 10 triệu");
        assertTrue(results.stream().allMatch(r -> r.document().price().compareTo(BigDecimal.valueOf(10_000_000)) <= 0),
                "Tất cả sản phẩm phải có giá <= 10 triệu");
        assertEquals("Samsung Galaxy Tab S9 FE", results.get(0).document().name());
    }

    @Test
    void testCriteriaColorRed() {
        // Hỏi "sản phẩm màu đỏ"
        List<ProductVectorStoreService.ScoredProduct> results = vectorStoreService.searchSemantic("sản phẩm màu đỏ", 5);

        assertFalse(results.isEmpty(), "Phải tìm thấy sản phẩm màu đỏ");
        assertEquals("Akko 3087 Dragon Ball", results.get(0).document().name());
    }

    @Test
    void testProductStatistics() {
        Map<String, Object> stats = vectorStoreService.getProductStatistics();
        assertNotNull(stats);
        assertEquals(3, stats.get("totalProducts"));
        assertEquals(63, stats.get("totalStock"));
    }

    @Test
    void testIncrementalUpsertAndRemoveProduct() {
        Category gamingLaptop = new Category();
        gamingLaptop.setId(3L);
        gamingLaptop.setName("Laptop");

        Product gamingProduct = new Product();
        gamingProduct.setId(99L);
        gamingProduct.setName("ASUS ROG Gaming RTX Laptop");
        gamingProduct.setDescription("Laptop gaming mạnh cho đồ họa và chơi game");
        gamingProduct.setPrice(BigDecimal.valueOf(8_990_000));
        gamingProduct.setQuantity(12);
        gamingProduct.setCategory(gamingLaptop);

        vectorStoreService.upsertProduct(gamingProduct);

        assertTrue(vectorStoreService.getAllDocuments().stream()
                .anyMatch(document -> document.productId().equals(99L)));

        List<ProductVectorStoreService.ScoredProduct> results =
                vectorStoreService.searchSemantic("laptop gaming dưới 10 triệu", 5);
        assertTrue(results.stream().anyMatch(result ->
                result.document().productId().equals(99L)));

        vectorStoreService.removeProduct(99L);
        assertFalse(vectorStoreService.getAllDocuments().stream()
                .anyMatch(document -> document.productId().equals(99L)));
    }
}
