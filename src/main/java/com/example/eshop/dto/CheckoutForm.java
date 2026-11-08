package com.example.eshop.dto;

import com.example.eshop.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutForm {
    @NotBlank(message = "Vui lÃ²ng nháº­p há» tÃªn ngÆ°á»i nháº­n")
    private String customerName;

    @NotBlank(message = "Vui lÃ²ng nháº­p sá»‘ Ä‘iá»‡n thoáº¡i")
    private String phone;

    @NotBlank(message = "Vui lÃ²ng nháº­p Ä‘á»‹a chá»‰ nháº­n hÃ ng")
    private String address;

    private String note;

    @NotNull(message = "Vui lÃ²ng chá»n phÆ°Æ¡ng thá»©c thanh toÃ¡n")
    private PaymentMethod paymentMethod = PaymentMethod.COD;
}