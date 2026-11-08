package com.example.eshop.controller;

import com.example.eshop.cart.Cart;
import com.example.eshop.dto.CheckoutForm;
import com.example.eshop.entity.Order;
import com.example.eshop.entity.PaymentMethod;
import com.example.eshop.repository.UserRepository;
import com.example.eshop.service.OrderService;
import com.example.eshop.service.UserTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final Cart cart;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final UserTrackingService userTrackingService;

    @GetMapping
    public String checkoutForm(Model model, Authentication auth) {
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        CheckoutForm form = new CheckoutForm();
        if (auth != null && auth.isAuthenticated()) {
            userRepository.findByUsername(auth.getName()).ifPresent(u -> {
                form.setCustomerName(u.getFullName());
                form.setPhone(u.getPhone());
                form.setAddress(u.getAddress());
            });
        }

        model.addAttribute("form", form);
        return "checkout/form";
    }

    @PostMapping
    public String placeOrder(
            @Valid @ModelAttribute("form") CheckoutForm form,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes ra,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) String sessionId
    ) {
        if (cart.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/cart";
        }

        if (bindingResult.hasErrors()) {
            return "checkout/form";
        }

        try {
            String username = auth != null ? auth.getName() : null;
            Order order = orderService.placeOrder(form, cart, username);
            cart.clear();
            trackPurchase(username, order, anonymousId, sessionId);

            if (order.getPaymentMethod() == PaymentMethod.BANKING) {
                return "redirect:/orders/" + order.getId() + "/payment";
            }

            ra.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn: #" + order.getId());
            return "redirect:/orders/" + order.getId() + "?success";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
    }

    private void trackPurchase(String username, Order order, String anonymousId, String sessionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "purchase");
        event.put("anonymousId", anonymousId);
        event.put("sessionId", sessionId);
        event.put("pageUrl", "/checkout");
        event.put("metadata", "{\"orderId\":" + order.getId()
                + ",\"totalAmount\":" + order.getTotalAmount().toPlainString()
                + ",\"paymentMethod\":\"" + order.getPaymentMethod().name() + "\"}");
        try {
            userTrackingService.track(username, event);
        } catch (RuntimeException e) {
            log.warn("Failed to record purchase event for order {}", order.getId(), e);
        }
    }
}
