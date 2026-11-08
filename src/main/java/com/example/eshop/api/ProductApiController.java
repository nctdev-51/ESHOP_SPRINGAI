package com.example.eshop.api;

import com.example.eshop.entity.Product;
import com.example.eshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<Product>> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "id,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(productService.search(keyword, categoryId, pageRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> create(@RequestBody @Valid Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.save(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> update(@PathVariable("id") Long id, @RequestBody @Valid Product product) {
        product.setId(id);
        return ResponseEntity.ok(productService.save(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") Long id) {
        productService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm thành công #" + id));
    }
}
