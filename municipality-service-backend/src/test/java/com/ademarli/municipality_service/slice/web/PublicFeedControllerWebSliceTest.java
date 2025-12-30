package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.PublicFeedController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.complaint.PublicFeedItem;
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
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicFeedController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicFeedControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ComplaintService complaintService;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void feed_ok_shouldReturn200_andPage() throws Exception {
        PublicFeedItem item = new PublicFeedItem(
                1L,
                "TRK-1",
                "Başlık",
                "Kategori",
                "Departman",
                ComplaintStatus.RESOLVED,
                OffsetDateTime.now(),
                "Cevap",
                4.5,
                9L
        );

        PageRequest pageable = PageRequest.of(0, 20);
        Page<PublicFeedItem> page = new PageImpl<>(List.of(item), pageable, 1);

        when(complaintService.publicFeed(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/public/feed?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].trackingCode").value("TRK-1"))
                .andExpect(jsonPath("$.content[0].title").value("Başlık"))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[0].avgRating").value(4.5))
                .andExpect(jsonPath("$.content[0].ratingCount").value(9));

        verify(complaintService).publicFeed(any(Pageable.class));
        verifyNoMoreInteractions(complaintService);
    }
}
