package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.PublicTrackingController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.complaint.PublicTrackingResponse;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.ComplaintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicTrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicTrackingControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ComplaintService complaintService;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void track_found_shouldReturn200_andBody() throws Exception {
        PublicTrackingResponse r = new PublicTrackingResponse();
        r.setTrackingCode("TRK-123");
        r.setStatus(ComplaintStatus.IN_REVIEW);
        r.setDepartmentName("Zabıta");

        when(complaintService.trackByCode("TRK-123")).thenReturn(Optional.of(r));

        mvc.perform(get("/api/public/complaints/track/TRK-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value("TRK-123"))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.departmentName").value("Zabıta"));

        verify(complaintService).trackByCode("TRK-123");
        verifyNoMoreInteractions(complaintService);
    }

    @Test
    void track_notFound_shouldReturn404() throws Exception {
        when(complaintService.trackByCode("NOPE")).thenReturn(Optional.empty());

        mvc.perform(get("/api/public/complaints/track/NOPE"))
                .andExpect(status().isNotFound());

        verify(complaintService).trackByCode("NOPE");
        verifyNoMoreInteractions(complaintService);
    }
}
