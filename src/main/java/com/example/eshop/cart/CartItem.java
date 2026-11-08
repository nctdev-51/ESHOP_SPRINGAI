package com.example.eshop.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private int quantity;

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}