package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends BaseIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    private RegisterRequest reg(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPhone(phone);
        r.setPassword(password);
        return r;
    }

    private LoginRequest login(String emailOrPhone, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmailOrPhone(emailOrPhone);
        r.setPassword(password);
        return r;
    }

    private User mkUser(String email, String phone, String rawPass, boolean enabled, Role... roles) {
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(rawPass));
        u.setEnabled(enabled);
        u.setRoles(Set.of(roles));
        return userRepository.save(u);
    }

    @Test
    void register_ok_should200_returnToken_andPersistCitizen() throws Exception {
        RegisterRequest req = reg("citizen@test.com", "5550000001", "Password123!");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());

        User saved = userRepository.findByEmail("citizen@test.com").orElseThrow();
        assertTrue(saved.isEnabled());
        assertTrue(saved.getRoles().contains(Role.CITIZEN));
        assertTrue(passwordEncoder.matches("Password123!", saved.getPasswordHash()));
    }

    @Test
    void register_duplicateEmail_should409_EMAIL_ALREADY_IN_USE() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", true, Role.CITIZEN);

        RegisterRequest req = reg("citizen@test.com", "5550000002", "Password123!");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void register_duplicatePhone_should409_PHONE_ALREADY_IN_USE() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", true, Role.CITIZEN);

        RegisterRequest req = reg("other@test.com", "5550000001", "Password123!");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_IN_USE"));
    }

    @Test
    void register_validation_should400_VALIDATION_FAILED() throws Exception {
        RegisterRequest req = reg("not-an-email", "", ""); // @Email + @NotBlank patlar

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }


    @Test
    void login_ok_byEmail_should200_returnToken() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", true, Role.CITIZEN);

        LoginRequest req = login("citizen@test.com", "Password123!");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());
    }

    @Test
    void login_ok_byPhone_should200_returnToken() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", true, Role.CITIZEN);

        LoginRequest req = login("5550000001", "Password123!");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());
    }

    @Test
    void login_userNotFound_should404_USER_NOT_FOUND() throws Exception {
        LoginRequest req = login("missing@test.com", "Password123!");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void login_wrongPassword_should409_INVALID_PASSWORD() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", true, Role.CITIZEN);

        LoginRequest req = login("citizen@test.com", "WrongPass!");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }

    @Test
    void login_disabledUser_should409_USER_DISABLED() throws Exception {
        mkUser("citizen@test.com", "5550000001", "Password123!", false, Role.CITIZEN);

        LoginRequest req = login("citizen@test.com", "Password123!");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DISABLED"));
    }

    @Test
    void login_validation_blankFields_should400_VALIDATION_FAILED() throws Exception {
        LoginRequest req = login("   ", "   "); // @NotBlank patlar

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.emailOrPhone").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
