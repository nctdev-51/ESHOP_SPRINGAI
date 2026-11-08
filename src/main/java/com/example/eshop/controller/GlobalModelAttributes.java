package com.example.eshop.controller;

import com.example.eshop.cart.Cart;
import com.example.eshop.entity.User;
import com.example.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final Cart cart;
    private final UserRepository userRepository;

    @ModelAttribute("cart")
    public Cart getCart() {
        return cart;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
