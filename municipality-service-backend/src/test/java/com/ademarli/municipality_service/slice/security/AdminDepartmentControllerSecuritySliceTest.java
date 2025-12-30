package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.AdminDepartmentController;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.JwtUtil;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AdminDepartmentService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDepartmentController.class)
@AutoConfigureMockMvc // ✅ filters açık
@Import({SecurityConfig.class, AdminDepartmentControllerSecuritySliceTest.TestBeans.class})
class AdminDepartmentControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminDepartmentService service;

    // ✅ SecurityConfig -> JwtFilter bean’i istiyor. JwtFilter -> JwtUtil + CustomUserDetailsService istiyor.
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;

    // ✅ ExceptionHandling’de kullanılıyor
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @BeforeEach
    void stubHandlers() throws Exception {
        // 401
        doAnswer(inv -> {
            HttpServletResponse resp = inv.getArgument(1);
            resp.setStatus(401);
            return null;
        }).when(restAuthEntryPoint).commence(any(), any(), any());

        // 403
        doAnswer(inv -> {
            HttpServletResponse resp = inv.getArgument(1);
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

    // ---- /api/admin/** => hasRole("ADMIN") ----

    @Test
    void create_withoutAuth_should401() throws Exception {
        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","active":true}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_withCitizen_should403() throws Exception {
        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","active":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withAdmin_should200() throws Exception {
        when(service.create(any())).thenReturn(new com.ademarli.municipality_service.model.dto.admin.DepartmentResponse());

        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","active":true}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void list_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/admin/departments?page=0&size=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_withAdmin_should200() throws Exception {
        when(service.list(any())).thenReturn(Page.empty());

        mvc.perform(get("/api/admin/departments?page=0&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    void update_withoutAuth_should401() throws Exception {
        mvc.perform(put("/api/admin/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"B","active":false}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withAdmin_should200() throws Exception {
        when(service.update(any(), any()))
                .thenReturn(new com.ademarli.municipality_service.model.dto.admin.DepartmentResponse());

        mvc.perform(put("/api/admin/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"B","active":false}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void delete_withoutAuth_should401() throws Exception {
        mvc.perform(delete("/api/admin/departments/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdmin_should200() throws Exception {
        mvc.perform(delete("/api/admin/departments/1"))
                .andExpect(status().isOk());
    }
}
