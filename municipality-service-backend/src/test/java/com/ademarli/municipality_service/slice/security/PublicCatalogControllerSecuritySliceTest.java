package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.PublicCatalogController;
import com.ademarli.municipality_service.security.*;
import com.ademarli.municipality_service.service.PublicCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCatalogController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, PublicCatalogControllerSecuritySliceTest.TestBeans.class})
class PublicCatalogControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PublicCatalogService service;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @BeforeEach
    void stubHandlers() throws Exception {
        doAnswer(inv -> {
            var resp = (jakarta.servlet.http.HttpServletResponse) inv.getArgument(1);
            resp.setStatus(401);
            return null;
        }).when(restAuthEntryPoint).commence(any(), any(), any());

        doAnswer(inv -> {
            var resp = (jakarta.servlet.http.HttpServletResponse) inv.getArgument(1);
            resp.setStatus(403);
            return null;
        }).when(restAccessDeniedHandler).handle(any(), any(), any());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JwtFilter jwtFilter(JwtUtil jwtUtil, CustomUserDetailsService uds) {
            return new JwtFilter(jwtUtil, uds);
        }
    }

    @Test
    void departments_withoutAuth_should200() throws Exception {
        when(service.activeDepartments()).thenReturn(List.of());

        mvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void departments_withAuth_should200() throws Exception {
        when(service.activeDepartments()).thenReturn(List.of());

        mvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk());
    }
}
