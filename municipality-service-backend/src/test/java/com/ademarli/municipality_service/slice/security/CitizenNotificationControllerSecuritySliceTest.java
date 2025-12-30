package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.CitizenNotificationController;
import com.ademarli.municipality_service.security.*;
import com.ademarli.municipality_service.service.CitizenNotificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CitizenNotificationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CitizenNotificationControllerSecuritySliceTest.TestBeans.class})
class CitizenNotificationControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CitizenNotificationService service;

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

    // ---------- GET /api/citizen/notifications ----------

    @Test
    void list_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/notifications?page=0&size=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void list_withAgent_should403() throws Exception {
        mvc.perform(get("/api/citizen/notifications?page=0&size=10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void list_withCitizen_should200() throws Exception {
        when(service.list(any(Long.class), any())).thenReturn(org.springframework.data.domain.Page.empty());

        mvc.perform(get("/api/citizen/notifications?page=0&size=10"))
                .andExpect(status().isOk());
    }

    // ---------- GET /unread-count ----------

    @Test
    void unreadCount_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unreadCount_withAdmin_should403() throws Exception {
        mvc.perform(get("/api/citizen/notifications/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void unreadCount_withCitizen_should200() throws Exception {
        when(service.unreadCount(123L)).thenReturn(0L);

        mvc.perform(get("/api/citizen/notifications/unread-count"))
                .andExpect(status().isOk());
    }

    // ---------- PATCH /{id}/read ----------

    @Test
    void markRead_withoutAuth_should401() throws Exception {
        mvc.perform(patch("/api/citizen/notifications/10/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void markRead_withAgent_should403() throws Exception {
        mvc.perform(patch("/api/citizen/notifications/10/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void markRead_withCitizen_should204() throws Exception {
        mvc.perform(patch("/api/citizen/notifications/10/read"))
                .andExpect(status().isNoContent());
    }

    // ---------- PATCH /read-all ----------

    @Test
    void markAllRead_withoutAuth_should401() throws Exception {
        mvc.perform(patch("/api/citizen/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void markAllRead_withAdmin_should403() throws Exception {
        mvc.perform(patch("/api/citizen/notifications/read-all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void markAllRead_withCitizen_should200() throws Exception {
        when(service.markAllRead(123L)).thenReturn(1);

        mvc.perform(patch("/api/citizen/notifications/read-all"))
                .andExpect(status().isOk());
    }
}
