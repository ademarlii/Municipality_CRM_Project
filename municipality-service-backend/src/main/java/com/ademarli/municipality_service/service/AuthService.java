package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.auth.AuthResponse;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim() : null;
        String phone = req.getPhone() != null ? req.getPhone().trim() : null;

        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("EMAIL_ALREADY_IN_USE");
        }
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("PHONE_ALREADY_IN_USE");
        }

        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRoles(Set.of(Role.CITIZEN));
        u.setEnabled(true);

        userRepository.save(u);
        return issueToken(u);
    }

    public AuthResponse login(LoginRequest req) {
        String raw = req.getEmailOrPhone() != null ? req.getEmailOrPhone().trim() : "";
        if (raw.isBlank()) throw new BusinessException("INVALID_CREDENTIALS");

        String emailKey = raw.contains("@") ? raw.toLowerCase() : raw;

        User user = userRepository.findByEmail(emailKey)
                .orElseGet(() -> userRepository.findByPhone(raw).orElse(null));

        if (user == null) throw new NotFoundException("USER_NOT_FOUND");
        if (!user.isEnabled()) throw new BusinessException("USER_DISABLED");

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD");
        }

        return issueToken(user);
    }
    private AuthResponse issueToken(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        return new AuthResponse(token, "Bearer", jwtUtil.getExpirationSeconds());
    }
}
