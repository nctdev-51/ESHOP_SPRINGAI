package com.example.eshop.service;

import com.example.eshop.dto.RegisterForm;
import com.example.eshop.entity.Role;
import com.example.eshop.entity.User;
import com.example.eshop.exception.NotFoundException;
import com.example.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng cÃ³ ID: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng: " + username));
    }

    @Transactional
    public User register(RegisterForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng");
        }

        User u = new User();
        u.setUsername(form.getUsername());
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setFullName(form.getFullName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setAddress(form.getAddress());
        u.setRole(Role.ROLE_CUSTOMER);
        u.setEnabled(true);

        return userRepository.save(u);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}