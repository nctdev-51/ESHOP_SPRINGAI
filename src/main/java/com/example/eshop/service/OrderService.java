package com.example.eshop.service;

import com.example.eshop.cart.Cart;
import com.example.eshop.cart.CartItem;
import com.example.eshop.dto.CheckoutForm;
import com.example.eshop.entity.*;
import com.example.eshop.exception.NotFoundException;
import com.example.eshop.repository.OrderRepository;
import com.example.eshop.repository.ProductRepository;
import com.example.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order placeOrder(CheckoutForm form, Cart cart, String username) {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Giá» hÃ ng Ä‘ang trá»‘ng");
        }

        Order order = new Order();
        order.setCustomerName(form.getCustomerName());
        order.setPhone(form.getPhone());
        order.setAddress(form.getAddress());
        order.setNote(form.getNote());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setStatus(OrderStatus.NEW);

        if (username != null) {
            order.setUser(userRepository.findByUsername(username).orElse(null));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Sáº£n pháº©m khÃ´ng cÃ²n bÃ¡n: " + item.getName()));

            if (p.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("\"" + p.getName() + "\" chá»‰ cÃ²n " + p.getQuantity() + " sáº£n pháº©m");
            }

            // Trá»« kho nguyÃªn tá»­
            p.setQuantity(p.getQuantity() - item.getQuantity());

            OrderDetail d = new OrderDetail();
            d.setProduct(p);
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(p.getPrice());

            order.addDetail(d);
            total = total.add(d.getSubtotal());
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n hÃ ng #" + id));
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public List<Order> findByUsername(String username) {
        return orderRepository.findByUserUsernameOrderByOrderDateDesc(username);
    }

    @Transactional
    public void markPaid(Long id) {
        Order o = findById(id);
        if (o.getStatus() == OrderStatus.NEW) {
            o.setStatus(OrderStatus.PAID);
        }
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public void updateStatus(Long id, OrderStatus status) {
        Order o = findById(id);
        if (status == OrderStatus.CANCELLED && o.getStatus() != OrderStatus.CANCELLED) {
            // HoÃ n láº¡i kho khi há»§y Ä‘Æ¡n
            o.getDetails().forEach(d -> 
                d.getProduct().setQuantity(d.getProduct().getQuantity() + d.getQuantity())
            );
        }
        o.setStatus(status);
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public void delete(Long id) {
        orderRepository.delete(findById(id));
    }
}