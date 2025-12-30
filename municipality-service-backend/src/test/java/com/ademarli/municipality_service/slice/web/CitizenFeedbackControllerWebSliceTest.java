package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.CitizenFeedbackController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.service.FeedbackService;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CitizenFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CitizenFeedbackControllerWebSliceTest {

    @Autowired MockMvc mvc;

    @MockitoBean
    FeedbackService feedbackService;

    // ✅ addFilters=false olsa bile bazı konfigürasyonlarda security beanleri istenebiliyor
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

    private FeedbackStatsResponse stats(long complaintId, double avg, long count, Integer myRating) {
        var r = new FeedbackStatsResponse();
        r.setComplaintId(complaintId);
        r.setAvgRating(avg);
        r.setRatingCount(count);
        r.setMyRating(myRating);
        return r;
    }

    @Test
    void upsertRating_ok_shouldReturn200_andBody() throws Exception {
        asCitizen("123");

        when(feedbackService.upsertRating(eq(123L), eq(10L), eq(5)))
                .thenReturn(stats(10L, 4.2, 10L, 5));

        mvc.perform(put("/api/citizen/feedback/10/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(10))
                .andExpect(jsonPath("$.avgRating").value(4.2))
                .andExpect(jsonPath("$.ratingCount").value(10))
                .andExpect(jsonPath("$.myRating").value(5));

        verify(feedbackService).upsertRating(123L, 10L, 5);
        verifyNoMoreInteractions(feedbackService);
    }

    @Test
    void myRating_ok_shouldReturn200_andInteger() throws Exception {
        asCitizen("123");

        when(feedbackService.getMyRating(123L, 10L)).thenReturn(4);

        mvc.perform(get("/api/citizen/feedback/10/rating/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));

        verify(feedbackService).getMyRating(123L, 10L);
        verifyNoMoreInteractions(feedbackService);
    }

    @Test
    void stats_ok_shouldReturn200_andBody() throws Exception {
        asCitizen("123");

        when(feedbackService.getStats(eq(10L), eq(123L)))
                .thenReturn(stats(10L, 3.7, 8L, 2));

        mvc.perform(get("/api/citizen/feedback/10/rating/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(10))
                .andExpect(jsonPath("$.avgRating").value(3.7))
                .andExpect(jsonPath("$.ratingCount").value(8))
                .andExpect(jsonPath("$.myRating").value(2));

        verify(feedbackService).getStats(10L, 123L);
        verifyNoMoreInteractions(feedbackService);
    }

    @Test
    void upsertRating_invalidJson_should400_invalidJson() throws Exception {
        asCitizen("123");

        mvc.perform(put("/api/citizen/feedback/10/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(feedbackService);
    }
}
