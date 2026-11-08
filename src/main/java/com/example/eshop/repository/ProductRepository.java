package com.example.eshop.repository;

import com.example.eshop.entity.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAllWithCategory();
}