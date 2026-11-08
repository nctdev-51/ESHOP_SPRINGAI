package com.example.eshop.controller;

import com.example.eshop.entity.Order;
import com.example.eshop.entity.Role;
import com.example.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public String myOrders(Authentication auth, Model model) {
        List<Order> orders = orderService.findByUsername(auth.getName());
        model.addAttribute("orders", orders);
        return "order/my-orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Authentication auth, Model model) {
        Order order = orderService.findById(id);

        boolean isOwner = order.getUser() != null && order.getUser().getUsername().equals(auth.getName());
        boolean isSeller = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ROLE_SELLER.name()));

        if (!isOwner && !isSeller) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này!");
        }

        model.addAttribute("order", order);
        return "order/detail";
    }

    @GetMapping("/{id}/payment")
    public String paymentPage(@PathVariable("id") Long id, Authentication auth, Model model) {
        Order order = orderService.findById(id);

        boolean isOwner = order.getUser() != null && order.getUser().getUsername().equals(auth.getName());
        boolean isSeller = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ROLE_SELLER.name()));

        if (!isOwner && !isSeller) {
            throw new AccessDeniedException("Bạn không có quyền truy cập trang thanh toán đơn này!");
        }

        model.addAttribute("order", order);
        return "order/payment";
    }

    @PostMapping("/{id}/pay")
    public String confirmPayment(@PathVariable("id") Long id, RedirectAttributes ra) {
        orderService.markPaid(id);
        ra.addFlashAttribute("successMessage", "Xác nhận thanh toán đơn hàng #" + id + " thành công!");
        return "redirect:/orders/" + id + "?paid";
    }
}
