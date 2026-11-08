package com.example.eshop.controller.admin;

import com.example.eshop.entity.Category;
import com.example.eshop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/category/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("category") Category category,
            BindingResult bindingResult,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            return "admin/category/form";
        }
        categoryService.save(category);
        ra.addFlashAttribute("successMessage", "Lưu danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "admin/category/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("category") Category category,
            BindingResult bindingResult,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            return "admin/category/form";
        }
        category.setId(id);
        categoryService.save(category);
        ra.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa danh mục #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa danh mục vì đang có sản phẩm thuộc danh mục này.");
        }
        return "redirect:/admin/categories";
    }
}
