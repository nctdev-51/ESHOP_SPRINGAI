package com.example.eshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "TÃªn sáº£n pháº©m khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Size(max = 150, message = "Tá»‘i Ä‘a 150 kÃ½ tá»±")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 500, message = "Tá»‘i Ä‘a 500 kÃ½ tá»±")
    private String description;

    @NotNull(message = "Vui lÃ²ng nháº­p giÃ¡")
    @DecimalMin(value = "0", message = "GiÃ¡ pháº£i >= 0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Vui lÃ²ng nháº­p sá»‘ lÆ°á»£ng")
    @Min(value = 0, message = "Sá»‘ lÆ°á»£ng pháº£i >= 0")
    @Column(nullable = false)
    private Integer quantity;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}