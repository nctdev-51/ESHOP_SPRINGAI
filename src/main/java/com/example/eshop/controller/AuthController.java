package com.example.eshop.controller;

import com.example.eshop.dto.RegisterForm;
import com.example.eshop.entity.Role;
import com.example.eshop.security.JwtService;
import com.example.eshop.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletResponse response,
            RedirectAttributes ra
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(Role.ROLE_CUSTOMER.name());

            String token = jwtService.generateToken(username, role);

            Cookie jwtCookie = new Cookie(JwtService.COOKIE_NAME, token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge((int) jwtService.getExpirationSeconds());
            response.addCookie(jwtCookie);

            if (Role.ROLE_SELLER.name().equals(role)) {
                return "redirect:/admin/products";
            }
            return "redirect:/";

        } catch (DisabledException e) {
            ra.addFlashAttribute("errorMessage", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
            return "redirect:/login";
        } catch (BadCredentialsException e) {
            ra.addFlashAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không đúng.");
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Đăng nhập thất bại: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes ra,
            Model model
    ) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.form", "Mật khẩu xác nhận không khớp");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
            ra.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đăng ký không thành công: " + e.getMessage());
            return "auth/register";
        }
    }
}
