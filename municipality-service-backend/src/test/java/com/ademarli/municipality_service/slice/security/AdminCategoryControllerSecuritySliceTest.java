package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.AdminCategoryController;
import com.ademarli.municipality_service.security.*;
import com.ademarli.municipality_service.service.AdminCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCategoryController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, AdminCategoryControllerSecuritySliceTest.TestBeans.class})
class AdminCategoryControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminCategoryService service;

    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    CustomUserDetailsService userDetailsService;

    @MockitoBean
    RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean
    RestAccessDeniedHandler restAccessDeniedHandler;

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
    void create_withoutAuth_should401() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","defaultDepartmentId":1,"active":true}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_withCitizen_should403() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","defaultDepartmentId":1,"active":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withAdmin_should200() throws Exception {
        when(service.create(any())).thenReturn(new com.ademarli.municipality_service.model.dto.admin.CategoryResponse());

        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","defaultDepartmentId":1,"active":true}
                                """))
                .andExpect(status().isOk());
    }
}
