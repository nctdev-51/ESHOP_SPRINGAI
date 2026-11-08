package com.example.eshop.controller.admin;

import com.example.eshop.entity.Order;
import com.example.eshop.entity.OrderStatus;
import com.example.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "admin/order/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Order order = orderService.findById(id);
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/order/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") OrderStatus status,
            RedirectAttributes ra
    ) {
        orderService.updateStatus(id, status);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng #" + id + " thành " + status.name());
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        orderService.delete(id);
        ra.addFlashAttribute("successMessage", "Đã xóa đơn hàng #" + id);
        return "redirect:/admin/orders";
    }
}
