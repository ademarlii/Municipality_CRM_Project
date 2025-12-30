package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.PublicTrackingController;
import com.ademarli.municipality_service.model.dto.complaint.PublicTrackingResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicTrackingController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, PublicTrackingControllerSecuritySliceTest.TestBeans.class})
class PublicTrackingControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ComplaintService complaintService;

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
    void track_withoutAuth_found_should200() throws Exception {
        PublicTrackingResponse r = new PublicTrackingResponse();
        r.setTrackingCode("TRK-1");
        r.setStatus(ComplaintStatus.NEW);
        r.setDepartmentName("Temizlik");

        when(complaintService.trackByCode("TRK-1")).thenReturn(Optional.of(r));

        mvc.perform(get("/api/public/complaints/track/TRK-1"))
                .andExpect(status().isOk());
    }

    @Test
    void track_withoutAuth_notFound_should404() throws Exception {
        when(complaintService.trackByCode("X")).thenReturn(Optional.empty());

        mvc.perform(get("/api/public/complaints/track/X"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void track_withAuth_shouldAlsoWork() throws Exception {
        when(complaintService.trackByCode("X")).thenReturn(Optional.empty());

        mvc.perform(get("/api/public/complaints/track/X"))
                .andExpect(status().isNotFound());
    }
}
