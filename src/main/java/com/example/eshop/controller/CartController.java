package com.example.eshop.controller;

import com.example.eshop.cart.Cart;
import com.example.eshop.entity.Product;
import com.example.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final Cart cart;
    private final ProductService productService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cart", cart);
        return "cart/view";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            RedirectAttributes ra
    ) {
        Product p = productService.findById(productId);
        if (p.getQuantity() < quantity) {
            ra.addFlashAttribute("errorMessage", "Sản phẩm \"" + p.getName() + "\" chỉ còn " + p.getQuantity() + " chiếc trong kho.");
            return "redirect:/products/" + productId;
        }

        cart.add(p, quantity);
        ra.addFlashAttribute("successMessage", "Đã thêm \"" + p.getName() + "\" vào giỏ hàng thành công!");
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity,
            RedirectAttributes ra
    ) {
        if (quantity > 0) {
            Product p = productService.findById(productId);
            if (p.getQuantity() < quantity) {
                ra.addFlashAttribute("errorMessage", "Chỉ còn tối đa " + p.getQuantity() + " chiếc trong kho.");
                return "redirect:/cart";
            }
        }
        cart.update(productId, quantity);
        ra.addFlashAttribute("successMessage", "Đã cập nhật số lượng thành công!");
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("productId") Long productId, RedirectAttributes ra) {
        cart.remove(productId);
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(RedirectAttributes ra) {
        cart.clear();
        ra.addFlashAttribute("successMessage", "Đã làm trống giỏ hàng.");
        return "redirect:/cart";
    }
}
