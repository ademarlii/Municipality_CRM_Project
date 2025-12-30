package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.AuthController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.auth.AuthResponse;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // ✅ security kapalı
@Import(GlobalExceptionHandler.class)     // ✅ INVALID_JSON / VALIDATION_FAILED body test için
class AuthControllerWebSliceTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthService authService;

    // addFilters=false olsa bile context bazen bu bean’leri arıyor -> garanti olsun
    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void register_ok_shouldReturn200_andBody() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token123", "Bearer", 3600));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","phone":"5551112233","password":"pass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_invalid_blankEmail_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"   ","phone":"5551112233","password":"pass123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_invalid_missingPhone_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"pass123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.phone").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_invalid_missingPassword_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","phone":"5551112233"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_invalidJson_should400_invalidJson() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailAlreadyExists_serviceThrowsBusiness_shouldReturnConflict() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new com.ademarli.municipality_service.exception.BusinessException("EMAIL_ALREADY_IN_USE"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"phone\":\"5551112233\",\"password\":\"pass123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }

    // -------------------- LOGIN --------------------

    @Test
    void login_ok_shouldReturn200_andBody() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token999", "Bearer", 7200));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"a@b.com","password":"pass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token999"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200));

        verify(authService).login(any(LoginRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_invalid_blankEmailOrPhone_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"   ","password":"pass123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.emailOrPhone").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void login_invalid_blankPassword_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"a@b.com","password":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void login_invalid_missingFields_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.emailOrPhone").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void login_invalidJson_should400_invalidJson() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone": }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenUserNotFound_serviceThrowsNotFound_shouldReturn404() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new com.ademarli.municipality_service.exception.NotFoundException("USER_NOT_FOUND"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailOrPhone\":\"no@user.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        verify(authService).login(any(LoginRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidPassword_serviceThrowsBusiness_shouldReturnConflict() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new com.ademarli.municipality_service.exception.BusinessException("INVALID_PASSWORD"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailOrPhone\":\"a@b.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        verify(authService).login(any(LoginRequest.class));
        verifyNoMoreInteractions(authService);
    }
}
