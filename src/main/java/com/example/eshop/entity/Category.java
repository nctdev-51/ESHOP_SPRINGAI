package com.example.eshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "TÃªn danh má»¥c khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Size(max = 100, message = "Tá»‘i Ä‘a 100 kÃ½ tá»±")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 255, message = "Tá»‘i Ä‘a 255 kÃ½ tá»±")
    private String description;
}