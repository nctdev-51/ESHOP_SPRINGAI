package com.example.eshop.controller.admin;

import com.example.eshop.dto.UserForm;
import com.example.eshop.entity.Role;
import com.example.eshop.entity.User;
import com.example.eshop.repository.UserRepository;
import com.example.eshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/user/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", Role.values());
        return "admin/user/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
            bindingResult.rejectValue("password", "error.userForm", "Mật khẩu không được để trống");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "error.userForm", "Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "error.userForm", "Email đã tồn tại");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user/form";
        }

        User u = new User();
        u.setUsername(form.getUsername());
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setFullName(form.getFullName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setAddress(form.getAddress());
        u.setRole(form.getRole());
        u.setEnabled(form.isEnabled());

        userService.save(u);
        ra.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        User u = userService.findById(id);
        UserForm form = new UserForm();
        form.setId(u.getId());
        form.setUsername(u.getUsername());
        form.setFullName(u.getFullName());
        form.setEmail(u.getEmail());
        form.setPhone(u.getPhone());
        form.setAddress(u.getAddress());
        form.setRole(u.getRole());
        form.setEnabled(u.isEnabled());

        model.addAttribute("userForm", form);
        model.addAttribute("roles", Role.values());
        return "admin/user/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user/form";
        }

        User u = userService.findById(id);
        u.setFullName(form.getFullName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setAddress(form.getAddress());
        u.setRole(form.getRole());
        u.setEnabled(form.isEnabled());

        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            u.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        userService.save(u);
        ra.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        User u = userService.findById(id);
        u.setEnabled(!u.isEnabled());
        userService.save(u);
        ra.addFlashAttribute("successMessage", "Đã " + (u.isEnabled() ? "mở khóa" : "khóa") + " tài khoản " + u.getUsername());
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            userService.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa tài khoản #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa tài khoản vì có dữ liệu liên kết.");
        }
        return "redirect:/admin/users";
    }
}
