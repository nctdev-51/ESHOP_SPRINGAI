package com.example.eshop.controller;

import com.example.eshop.entity.Category;
import com.example.eshop.entity.Product;
import com.example.eshop.service.CategoryService;
import com.example.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/products"})
    public String index(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productService.search(keyword, categoryId, pageRequest);
        List<Category> categories = categoryService.findAll();

        model.addAttribute("products", productPage);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        return "shop/index";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "shop/detail";
    }
}
