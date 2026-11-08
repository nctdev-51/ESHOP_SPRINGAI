package com.example.eshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {
    @NotBlank(message = "Vui lÃ²ng nháº­p tÃªn Ä‘Äƒng nháº­p")
    @Size(min = 4, max = 50, message = "TÃªn Ä‘Äƒng nháº­p tá»« 4 - 50 kÃ½ tá»±")
    private String username;

    @NotBlank(message = "Vui lÃ²ng nháº­p máº­t kháº©u")
    @Size(min = 6, message = "Máº­t kháº©u tá»‘i thiá»ƒu 6 kÃ½ tá»±")
    private String password;

    @NotBlank(message = "Vui lÃ²ng xÃ¡c nháº­n máº­t kháº©u")
    private String confirmPassword;

    @NotBlank(message = "Vui lÃ²ng nháº­p há» vÃ  tÃªn")
    private String fullName;

    @NotBlank(message = "Vui lÃ²ng nháº­p email")
    @Email(message = "Email khÃ´ng há»£p lá»‡")
    private String email;

    private String phone;
    private String address;
}