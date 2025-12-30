package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.CitizenNotificationController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.notification.NotificationItemResponse;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.CitizenNotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CitizenNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CitizenNotificationControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CitizenNotificationService service;

    // ✅ addFilters=false olunca spring bazen security beanleri ister → mockla geç
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

    private NotificationItemResponse item(long id, boolean isRead) {
        var n = new NotificationItemResponse();
        n.setId(id);
        n.setComplaintId(10L);
        n.setTitle("Title " + id);
        n.setBody("Body " + id);
        n.setLink("/complaints/10");
        n.setRead(isRead);
        n.setCreatedAt(OffsetDateTime.now());
        return n;
    }

    @Test
    void list_ok_shouldReturn200_andPage() throws Exception {
        asCitizen("123");

        PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<NotificationItemResponse> page = new PageImpl<>(
                List.of(item(1, false), item(2, true)),
                pageable,
                2
        );

        when(service.list(eq(123L), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/citizen/notifications?page=0&size=10&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].complaintId").value(10))
                .andExpect(jsonPath("$.content[0].title").value("Title 1"))
                .andExpect(jsonPath("$.content[0].body").value("Body 1"))
                .andExpect(jsonPath("$.content[0].link").value("/complaints/10"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        verify(service).list(eq(123L), any(Pageable.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void unreadCount_ok_shouldReturn200_andBody() throws Exception {
        asCitizen("123");

        when(service.unreadCount(123L)).thenReturn(7L);

        mvc.perform(get("/api/citizen/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));

        verify(service).unreadCount(123L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void markRead_ok_shouldReturn204() throws Exception {
        asCitizen("123");

        mvc.perform(patch("/api/citizen/notifications/55/read"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).markRead(123L, 55L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void markAllRead_ok_shouldReturn200_andBody() throws Exception {
        asCitizen("123");

        when(service.markAllRead(123L)).thenReturn(4);

        mvc.perform(patch("/api/citizen/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));

        verify(service).markAllRead(123L);
        verifyNoMoreInteractions(service);
    }
}
