package com.example.eshop.ai;

import com.example.eshop.cart.Cart;
import com.example.eshop.dto.CheckoutForm;
import com.example.eshop.entity.Order;
import com.example.eshop.entity.PaymentMethod;
import com.example.eshop.entity.Product;
import com.example.eshop.repository.ProductRepository;
import com.example.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI Tools (Function Calling theo Slide 11, 15, 16):
 * Cho phép AI Model gọi trực tiếp các phương thức Java để tra cứu Vector Database và đặt hàng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductTools {

    private final ProductVectorStoreService vectorStoreService;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public record ProductSummary(
            Long id,
            String name,
            BigDecimal price,
            int quantity,
            String categoryName,
            String color,
            double similarityScore,
            String description
    ) {}

    @Tool(description = "Tìm kiếm sản phẩm theo ngữ nghĩa trong Vector Database sử dụng độ tương đồng Cosine (Cosine Similarity) kết hợp lọc theo tiêu chí (giá, màu sắc, mục đích)")
    public List<ProductSummary> semanticSearchProducts(
            @ToolParam(description = "Câu hỏi hoặc yêu cầu tìm kiếm ngữ nghĩa của khách hàng, ví dụ: 'máy tính dưới 10 triệu', 'laptop gaming', 'sản phẩm màu đỏ'") String query) {
        log.info("Gọi Tool semanticSearchProducts với query: '{}'", query);
        List<ProductVectorStoreService.ScoredProduct> scored = vectorStoreService.searchSemantic(query, 12);

        return scored.stream().map(sp -> {
            var doc = sp.document();
            return new ProductSummary(
                    doc.productId(),
                    doc.name(),
                    doc.price(),
                    doc.quantity(),
                    doc.categoryName(),
                    doc.color(),
                    Math.round(sp.similarityScore() * 100.0) / 100.0,
                    doc.description()
            );
        }).collect(Collectors.toList());
    }

    @Tool(description = "Thống kê sản phẩm eShop theo các tiêu chí: phân bố khoảng giá, danh mục sản phẩm, tình trạng tồn kho")
    public Map<String, Object> getProductStatistics(
            @ToolParam(description = "Tiêu chí cần thống kê, ví dụ: 'all', 'price', 'category', 'stock'") String criteria) {
        log.info("Gọi Tool getProductStatistics với criteria: '{}'", criteria);
        return vectorStoreService.getProductStatistics();
    }

    @Tool(description = "Tìm kiếm cơ bản sản phẩm trong kho theo từ khóa")
    public List<ProductSummary> searchProducts(
            @ToolParam(description = "Từ khóa tìm kiếm") String keyword) {
        return semanticSearchProducts(keyword);
    }

    @Tool(description = "Tạo đơn hàng nhanh cho khách hàng khi khách đã xác nhận thông tin và đồng ý đặt hàng")
    public String placeOrderFromChat(
            @ToolParam(description = "ID của sản phẩm muốn mua") Long productId,
            @ToolParam(description = "Số lượng muốn mua (mặc định là 1)") int quantity,
            @ToolParam(description = "Họ tên người nhận") String customerName,
            @ToolParam(description = "Số điện thoại nhận hàng") String phone,
            @ToolParam(description = "Địa chỉ nhận hàng") String address,
            @ToolParam(description = "Username đăng nhập (nếu có)") String username) {

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm có ID: " + productId));

        if (p.getQuantity() < quantity) {
            return "Đặt hàng thất bại: Sản phẩm " + p.getName() + " chỉ còn lại " + p.getQuantity() + " chiếc trong kho.";
        }

        Cart tempCart = new Cart();
        tempCart.add(p, quantity);

        CheckoutForm form = new CheckoutForm();
        form.setCustomerName(customerName);
        form.setPhone(phone);
        form.setAddress(address);
        form.setNote("Đơn hàng tạo tự động qua Trợ Lý AI eShop (Vector Database)");
        form.setPaymentMethod(PaymentMethod.COD);

        Order order = orderService.placeOrder(form, tempCart, username);
        return String.format("Đặt hàng thành công! Đơn hàng #%d đã được tạo cho %s (%s). Tổng thanh toán: %,.0f VNĐ. Phương thức COD.",
                order.getId(), form.getCustomerName(), form.getPhone(), order.getTotalAmount());
    }
}
