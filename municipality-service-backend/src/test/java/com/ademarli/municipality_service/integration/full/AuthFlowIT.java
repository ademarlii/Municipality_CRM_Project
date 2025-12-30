package com.ademarli.municipality_service.integration.full;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.AuthResponse;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIT extends BaseIntegrationTest {

    private AuthResponse register(String email, String phone, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword(password);

        String json = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andReturn().getResponse().getContentAsString();

        return om.readValue(json, AuthResponse.class);
    }

    private AuthResponse login(String emailOrPhone, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmailOrPhone(emailOrPhone);
        req.setPassword(password);

        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        return om.readValue(json, AuthResponse.class);
    }

    @Test
    void scenario1_register_then_login_shouldReturnTokens() throws Exception {
        AuthResponse reg = register("citizen1@test.com", "5551112233", "Password123!");
        assertThat(reg.accessToken()).isNotBlank();

        AuthResponse log = login("citizen1@test.com", "Password123!");
        assertThat(log.accessToken()).isNotBlank();
    }

    @Test
    void scenario1_register_invalidEmail_should400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("not-an-email");
        req.setPhone("5551112233");
        req.setPassword("Password123!");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scenario1_login_blankFields_should400() throws Exception {

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"", "password":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
