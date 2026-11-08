package com.example.eshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController {

    @RequestMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
