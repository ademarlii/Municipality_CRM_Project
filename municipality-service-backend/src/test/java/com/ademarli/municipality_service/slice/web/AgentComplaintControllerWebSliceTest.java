package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.AgentComplaintController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.agent.AgentComplaintListItem;
import com.ademarli.municipality_service.model.dto.complaint.ChangeStatusRequest;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AgentComplaintService;
import com.ademarli.municipality_service.service.ComplaintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgentComplaintController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AgentComplaintControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AgentComplaintService agentComplaintService;

    @MockitoBean
    ComplaintService complaintService;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    private void asAgent(String userId) {
        var auths = List.of(new SimpleGrantedAuthority("ROLE_AGENT"));
        var principal = new User(userId, "N/A", auths);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, auths);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private AgentComplaintListItem item(long id, ComplaintStatus status) {
        AgentComplaintListItem i = new AgentComplaintListItem();
        i.setId(id);
        i.setTrackingCode("TRK-" + id);
        i.setTitle("Başlık " + id);
        i.setStatus(status);
        i.setCreatedAt(OffsetDateTime.now());
        i.setCategoryName("Kategori");
        i.setDepartmentName("Departman");
        i.setCitizenEmail("citizen@test.com");
        return i;
    }

    @Test
    void list_ok_withoutStatus_shouldReturn200_andPassNullStatuses() throws Exception {
        asAgent("123");

        PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<AgentComplaintListItem> page = new PageImpl<>(
                List.of(item(1, ComplaintStatus.NEW)),
                pageable,
                1
        );

        when(agentComplaintService.listForAgent(eq(123L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/agent/complaints?page=0&size=10&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].trackingCode").value("TRK-1"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));

        verify(agentComplaintService).listForAgent(eq(123L), isNull(), isNull(), any(Pageable.class));
        verifyNoMoreInteractions(agentComplaintService);
        verifyNoInteractions(complaintService);
    }

    @Test
    void list_ok_withStatus_shouldReturn200_andPassStatusesList() throws Exception {
        asAgent("123");

        PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<AgentComplaintListItem> page = new PageImpl<>(
                List.of(item(7, ComplaintStatus.NEW)),
                pageable,
                1
        );

        when(agentComplaintService.listForAgent(eq(123L), eq("abc"), eq(List.of(ComplaintStatus.NEW)), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/agent/complaints?q=abc&status=NEW&page=0&size=10&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));

        verify(agentComplaintService).listForAgent(eq(123L), eq("abc"), eq(List.of(ComplaintStatus.NEW)), any(Pageable.class));
        verifyNoMoreInteractions(agentComplaintService);
        verifyNoInteractions(complaintService);
    }

    @Test
    void changeStatus_ok_shouldReturn200() throws Exception {
        asAgent("123");

        mvc.perform(post("/api/agent/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"RESOLVED","note":"ok","publicAnswer":"done"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(complaintService).changeStatus(eq(123L), eq(10L), eq(ComplaintStatus.RESOLVED), eq("ok"), eq("done"));
        verifyNoMoreInteractions(complaintService);
        verifyNoInteractions(agentComplaintService);
    }

    @Test
    void changeStatus_blankToStatus_should400_validationFailed() throws Exception {
        asAgent("123");

        mvc.perform(post("/api/agent/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.toStatus").exists());

        verifyNoInteractions(complaintService);
        verifyNoInteractions(agentComplaintService);
    }

    @Test
    void changeStatus_emptyJson_should400_validationFailed() throws Exception {
        asAgent("123");

        mvc.perform(post("/api/agent/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.toStatus").exists());

        verifyNoInteractions(complaintService);
        verifyNoInteractions(agentComplaintService);
    }

    @Test
    void changeStatus_invalidJson_should400_invalidJson() throws Exception {
        asAgent("123");

        mvc.perform(post("/api/agent/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(complaintService);
        verifyNoInteractions(agentComplaintService);
    }

    @Test
    void changeStatus_invalidEnum_should400_badRequest_orUnexpectedDependingOnHandler() throws Exception {
        asAgent("123");

        mvc.perform(post("/api/agent/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"XYZ"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(complaintService);
        verifyNoInteractions(agentComplaintService);
    }
}

