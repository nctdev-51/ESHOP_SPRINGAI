package com.example.eshop.repository;

import com.example.eshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByOrderDateDesc();
    List<Order> findByUserUsernameOrderByOrderDateDesc(String username);
}