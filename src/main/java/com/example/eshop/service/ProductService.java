package com.example.eshop.service;

import com.example.eshop.entity.Product;
import com.example.eshop.exception.NotFoundException;
import com.example.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Page<Product> search(String keyword, Long categoryId, Pageable pageable) {
        return productRepository.search(keyword, categoryId, pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y sáº£n pháº©m cÃ³ ID: " + id));
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}