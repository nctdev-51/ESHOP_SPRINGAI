package com.example.eshop.api;

import com.example.eshop.dto.RegisterForm;
import com.example.eshop.entity.User;
import com.example.eshop.security.JwtService;
import com.example.eshop.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private long expiresIn;
        private String username;
        private String role;

        public AuthResponse(String token, long expiresIn, String username, String role) {
            this.token = token;
            this.expiresIn = expiresIn;
            this.username = username;
            this.role = role;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_CUSTOMER");

        String token = jwtService.generateToken(request.getUsername(), role);
        long expiresIn = jwtService.getExpirationSeconds();

        return ResponseEntity.ok(new AuthResponse(token, expiresIn, request.getUsername(), role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterForm form) {
        User user = userService.register(form);
        return ResponseEntity.ok(Map.of(
                "message", "Đăng ký tài khoản thành công",
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }
}
