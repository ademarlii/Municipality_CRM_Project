package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.AgentComplaintController;
import com.ademarli.municipality_service.model.dto.agent.AgentComplaintListItem;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.JwtUtil;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AgentComplaintService;
import com.ademarli.municipality_service.service.ComplaintService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgentComplaintController.class)
@AutoConfigureMockMvc // ✅ filters açık
@Import({SecurityConfig.class, AgentComplaintControllerSecuritySliceTest.TestBeans.class})
class AgentComplaintControllerSecuritySliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AgentComplaintService agentComplaintService;

    @MockitoBean
    ComplaintService complaintService;

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

    private AgentComplaintListItem item(long id) {
        AgentComplaintListItem x = new AgentComplaintListItem();
        x.setId(id);
        x.setTrackingCode("TRK-" + id);
        x.setTitle("Şikayet " + id);
        x.setStatus(ComplaintStatus.NEW);
        x.setCreatedAt(OffsetDateTime.parse("2025-12-27T10:00:00+03:00"));
        x.setCategoryName("Kategori");
        x.setDepartmentName("Departman");
        x.setCitizenEmail("citizen@test.com");
        return x;
    }

    // -------------------- /api/agent/** => hasAnyRole(ADMIN,AGENT) --------------------

    @Test
    void list_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/agent/complaints"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN", username = "123")
    void list_withCitizen_should403() throws Exception {
        mvc.perform(get("/api/agent/complaints"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENT", username = "123") // ✅ username sayısal olmalı!
    void list_withAgent_should200() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<AgentComplaintListItem> page = new PageImpl<>(List.of(item(1)), pageable, 1);

        when(agentComplaintService.listForAgent(eq(123L), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/agent/complaints?page=0&size=20"))
                .andExpect(status().isOk());
    }

    @Test
    void changeStatus_withoutAuth_should401() throws Exception {
        mvc.perform(post("/api/agent/complaints/77/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"RESOLVED"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN", username = "123")
    void changeStatus_withCitizen_should403() throws Exception {
        mvc.perform(post("/api/agent/complaints/77/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"RESOLVED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENT", username = "123")
    void changeStatus_withAgent_should200() throws Exception {
        mvc.perform(post("/api/agent/complaints/77/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"RESOLVED","note":"ok","publicAnswer":"done"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "123")
    void changeStatus_withAdmin_should200() throws Exception {
        mvc.perform(post("/api/agent/complaints/77/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"CLOSED"}
                                """))
                .andExpect(status().isOk());
    }
}
