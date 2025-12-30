package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.PublicFeedbackController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicFeedbackControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FeedbackService feedbackService;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void stats_ok_shouldReturn200_andBody() throws Exception {
        FeedbackStatsResponse r = new FeedbackStatsResponse();
        r.setComplaintId(10L);
        r.setAvgRating(4.2);
        r.setRatingCount(12L);
        r.setMyRating(null);

        when(feedbackService.getStats(10L, null)).thenReturn(r);

        mvc.perform(get("/api/public/complaints/10/rating/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(10))
                .andExpect(jsonPath("$.avgRating").value(4.2))
                .andExpect(jsonPath("$.ratingCount").value(12))
                .andExpect(jsonPath("$.myRating").doesNotExist()); // null olduğu için bazen serialize edilmez

        verify(feedbackService).getStats(10L, null);
        verifyNoMoreInteractions(feedbackService);
    }
}
