package com.example.eshop.dto;

import com.example.eshop.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserForm {
    private Long id;

    @NotBlank(message = "Vui lÃ²ng nháº­p tÃªn Ä‘Äƒng nháº­p")
    private String username;

    private String password;

    @NotBlank(message = "Vui lÃ²ng nháº­p há» vÃ  tÃªn")
    private String fullName;

    @NotBlank(message = "Vui lÃ²ng nháº­p email")
    @Email(message = "Email khÃ´ng Ä‘Ãºng Ä‘á»‹nh dáº¡ng")
    private String email;

    private String phone;
    private String address;
    private Role role = Role.ROLE_CUSTOMER;
    private boolean enabled = true;
}