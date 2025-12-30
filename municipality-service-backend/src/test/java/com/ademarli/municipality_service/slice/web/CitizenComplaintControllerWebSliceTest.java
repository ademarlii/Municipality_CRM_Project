package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.CitizenComplaintController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.complaint.ComplaintDetailResponse;
import com.ademarli.municipality_service.model.dto.complaint.ComplaintSummaryResponse;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.ComplaintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CitizenComplaintController.class)
@AutoConfigureMockMvc(addFilters = false) // ✅ filtre yok -> principal için SecurityContextHolder basacağız
@Import(GlobalExceptionHandler.class)
class CitizenComplaintControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ComplaintService complaintService;

    // ✅ addFilters=false olunca bazen security beanleri ister → mockla geç
    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    private void asCitizen(String userId) {
        var auths = List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"));
        var principal = new User(userId, "N/A", auths);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, auths);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private ComplaintDetailResponse detail(long id) {
        ComplaintDetailResponse r = new ComplaintDetailResponse();
        r.setId(id);
        r.setTrackingCode("TRK-" + id);
        r.setTitle("Başlık " + id);
        r.setDescription("Açıklama");
        r.setStatus(ComplaintStatus.NEW);
        r.setCategoryId(5L);
        r.setDepartmentId(2L);
        r.setCreatedAt(OffsetDateTime.now());
        return r;
    }

    private ComplaintSummaryResponse summary(long id) {
        ComplaintSummaryResponse r = new ComplaintSummaryResponse();
        r.setId(id);
        r.setTrackingCode("TRK-" + id);
        r.setTitle("Başlık " + id);
        r.setStatus(ComplaintStatus.NEW);
        r.setCreatedAt(OffsetDateTime.now());
        return r;
    }

    @Test
    void create_ok_shouldReturn200_andBody() throws Exception {
        asCitizen("123");

        when(complaintService.createComplaint(eq(123L), any()))
                .thenReturn(detail(10));

        mvc.perform(post("/api/citizen/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Çöp sorunu","description":"Mahallede çöp var","categoryId":5,"lat":38.12,"lon":27.11}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.trackingCode").value("TRK-10"))
                .andExpect(jsonPath("$.title").value("Başlık 10"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.categoryId").value(5));

        verify(complaintService).createComplaint(eq(123L), any());
        verifyNoMoreInteractions(complaintService);
    }

    @Test
    void create_blankTitle_should400_validationFailed() throws Exception {
        asCitizen("123");

        mvc.perform(post("/api/citizen/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","categoryId":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verifyNoInteractions(complaintService);
    }

    @Test
    void create_missingCategoryId_should400_validationFailed() throws Exception {
        asCitizen("123");

        mvc.perform(post("/api/citizen/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"A"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        verifyNoInteractions(complaintService);
    }

    @Test
    void create_invalidJson_should400_invalidJson() throws Exception {
        asCitizen("123");

        mvc.perform(post("/api/citizen/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(complaintService);
    }

    @Test
    void myComplaints_ok_shouldReturn200_andList() throws Exception {
        asCitizen("123");

        when(complaintService.listMyComplaints(123L))
                .thenReturn(List.of(summary(1), summary(2)));

        mvc.perform(get("/api/citizen/complaints/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].trackingCode").value("TRK-1"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(complaintService).listMyComplaints(123L);
        verifyNoMoreInteractions(complaintService);
    }

    @Test
    void getOne_ok_shouldReturn200() throws Exception {
        asCitizen("123");

        when(complaintService.getMyComplaintOrThrow(eq(10L), eq(123L)))
                .thenReturn(detail(10));

        mvc.perform(get("/api/citizen/complaints/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.trackingCode").value("TRK-10"));

        verify(complaintService).getMyComplaintOrThrow(10L, 123L);
        verifyNoMoreInteractions(complaintService);
    }
}
