package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.CitizenComplaintController;
import com.ademarli.municipality_service.model.dto.complaint.ComplaintDetailResponse;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.security.*;
import com.ademarli.municipality_service.service.ComplaintService;
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

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CitizenComplaintController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CitizenComplaintControllerSecuritySliceTest.TestBeans.class})
class CitizenComplaintControllerSecuritySliceTest {

    @Autowired MockMvc mvc;

    @MockitoBean ComplaintService complaintService;

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

    private ComplaintDetailResponse detail(long id) {
        ComplaintDetailResponse r = new ComplaintDetailResponse();
        r.setId(id);
        r.setTrackingCode("TRK-" + id);
        r.setTitle("Başlık " + id);
        r.setStatus(ComplaintStatus.NEW);
        r.setCategoryId(7L);
        r.setDepartmentId(3L);
        r.setCreatedAt(OffsetDateTime.parse("2025-12-27T10:00:00+03:00"));
        return r;
    }

    // ---- CREATE ----
    @Test
    void create_withoutAuth_should401() throws Exception {
        mvc.perform(post("/api/citizen/complaints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"A","description":"x","categoryId":7}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "123", roles = "AGENT")
    void create_withWrongRole_should403() throws Exception {
        mvc.perform(post("/api/citizen/complaints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"A","description":"x","categoryId":7}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void create_withCitizen_should200() throws Exception {
        when(complaintService.createComplaint(eq(123L), any()))
                .thenReturn(detail(1));

        mvc.perform(post("/api/citizen/complaints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"A","description":"x","categoryId":7}
                                """))
                .andExpect(status().isOk());
    }

    // ---- MY ----
    @Test
    void my_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/complaints/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "123", roles = "AGENT")
    void my_withWrongRole_should403() throws Exception {
        mvc.perform(get("/api/citizen/complaints/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void my_withCitizen_should200() throws Exception {
        when(complaintService.listMyComplaints(123L)).thenReturn(java.util.List.of());
        mvc.perform(get("/api/citizen/complaints/my"))
                .andExpect(status().isOk());
    }

    @Test
    void getOne_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/complaints/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "123", roles = "AGENT")
    void getOne_withWrongRole_should403() throws Exception {
        mvc.perform(get("/api/citizen/complaints/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void getOne_withCitizen_should200() throws Exception {
        when(complaintService.getMyComplaintOrThrow(10L, 123L)).thenReturn(detail(10));
        mvc.perform(get("/api/citizen/complaints/10"))
                .andExpect(status().isOk());
    }
}
