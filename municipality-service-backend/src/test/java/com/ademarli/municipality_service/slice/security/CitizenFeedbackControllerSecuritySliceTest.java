package com.ademarli.municipality_service.slice.security;

import com.ademarli.municipality_service.config.SecurityConfig;
import com.ademarli.municipality_service.controller.CitizenFeedbackController;
import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.security.*;
import com.ademarli.municipality_service.service.FeedbackService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CitizenFeedbackController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CitizenFeedbackControllerSecuritySliceTest.TestBeans.class})
class CitizenFeedbackControllerSecuritySliceTest {

    @Autowired MockMvc mvc;

    @MockitoBean FeedbackService feedbackService;

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
    void upsertRating_withoutAuth_should401() throws Exception {
        mvc.perform(put("/api/citizen/feedback/10/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void upsertRating_withAgent_should403() throws Exception {
        mvc.perform(put("/api/citizen/feedback/10/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void upsertRating_withCitizen_should200() throws Exception {
        var res = new FeedbackStatsResponse();
        res.setComplaintId(10L);
        res.setAvgRating(4.0);
        res.setRatingCount(1L);
        res.setMyRating(5);

        when(feedbackService.upsertRating(eq(123L), eq(10L), eq(5))).thenReturn(res);

        mvc.perform(put("/api/citizen/feedback/10/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void myRating_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/feedback/10/rating/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void myRating_withAgent_should403() throws Exception {
        mvc.perform(get("/api/citizen/feedback/10/rating/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void myRating_withCitizen_should200() throws Exception {
        when(feedbackService.getMyRating(123L, 10L)).thenReturn(4);

        mvc.perform(get("/api/citizen/feedback/10/rating/me"))
                .andExpect(status().isOk());
    }

    @Test
    void stats_withoutAuth_should401() throws Exception {
        mvc.perform(get("/api/citizen/feedback/10/rating/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_withAdmin_should403() throws Exception {
        mvc.perform(get("/api/citizen/feedback/10/rating/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "123", roles = "CITIZEN")
    void stats_withCitizen_should200() throws Exception {
        var res = new FeedbackStatsResponse();
        res.setComplaintId(10L);
        res.setAvgRating(3.5);
        res.setRatingCount(2L);
        res.setMyRating(2);

        when(feedbackService.getStats(10L, 123L)).thenReturn(res);

        mvc.perform(get("/api/citizen/feedback/10/rating/stats"))
                .andExpect(status().isOk());
    }
}
