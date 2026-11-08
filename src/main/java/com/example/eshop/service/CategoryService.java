package com.example.eshop.service;

import com.example.eshop.entity.Category;
import com.example.eshop.exception.NotFoundException;
import com.example.eshop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y danh má»¥c cÃ³ ID: " + id));
    }

    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}