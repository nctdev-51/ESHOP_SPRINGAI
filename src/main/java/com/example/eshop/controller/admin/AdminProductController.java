package com.example.eshop.controller.admin;

import com.example.eshop.entity.Product;
import com.example.eshop.service.CategoryService;
import com.example.eshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> productPage = productService.search(keyword, categoryId, pageRequest);

        model.addAttribute("products", productPage);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        return "admin/product/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/product/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "admin/product/form";
        }
        productService.save(product);
        ra.addFlashAttribute("successMessage", "Lưu sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        return "admin/product/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "admin/product/form";
        }
        product.setId(id);
        productService.save(product);
        ra.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            productService.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm vì đã có đơn hàng liên quan.");
        }
        return "redirect:/admin/products";
    }
}
