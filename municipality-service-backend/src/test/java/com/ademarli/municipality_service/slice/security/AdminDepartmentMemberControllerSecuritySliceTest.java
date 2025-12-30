package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.AdminDepartmentMemberController;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.JwtUtil;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AdminDepartmentMemberService;
import jakarta.servlet.http.HttpServletResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDepartmentMemberController.class)
@AutoConfigureMockMvc // ✅ filters açık
@Import({SecurityConfig.class, AdminDepartmentMemberControllerSecuritySliceTest.TestBeans.class})
class AdminDepartmentMemberControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminDepartmentMemberService service;

    // ✅ JwtFilter bağımlılıkları
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;

    // ✅ SecurityConfig exception handling
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
    void add_withoutAuth_should401() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"memberRole":"MEMBER"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void add_withCitizen_should403() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"memberRole":"MEMBER"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void add_withAdmin_should200() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"memberRole":"MEMBER"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void list_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/admin/departments/10/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_withAdmin_should200() throws Exception {
        mvc.perform(get("/api/admin/departments/10/members"))
                .andExpect(status().isOk());
    }

    @Test
    void remove_withoutAuth_should401() throws Exception {
        mvc.perform(delete("/api/admin/departments/10/members/99"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void remove_withAdmin_should200() throws Exception {
        mvc.perform(delete("/api/admin/departments/10/members/99"))
                .andExpect(status().isOk());
    }

    @Test
    void changeRole_withoutAuth_should401() throws Exception {
        mvc.perform(patch("/api/admin/departments/10/members/99/role/MANAGER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeRole_withAdmin_should200() throws Exception {
        mvc.perform(patch("/api/admin/departments/10/members/99/role/MANAGER"))
                .andExpect(status().isOk());
    }
}
