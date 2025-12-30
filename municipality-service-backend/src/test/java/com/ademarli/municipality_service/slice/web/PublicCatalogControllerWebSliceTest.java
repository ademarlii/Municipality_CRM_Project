package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.PublicCatalogController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.publiccatalog.PublicCategoryItem;
import com.ademarli.municipality_service.model.dto.publiccatalog.PublicDepartmentItem;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.PublicCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicCatalogControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PublicCatalogService service;

    // addFilters=false olunca bazen security beanleri ister
    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void departments_ok_shouldReturn200_andList() throws Exception {
        when(service.activeDepartments()).thenReturn(List.of(
                new PublicDepartmentItem(1L, "Zabıta"),
                new PublicDepartmentItem(2L, "Temizlik")
        ));

        mvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Zabıta"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Temizlik"));

        verify(service).activeDepartments();
        verifyNoMoreInteractions(service);
    }

    @Test
    void categories_ok_shouldReturn200_andList() throws Exception {
        when(service.activeCategoriesByDepartment(5L)).thenReturn(List.of(
                new PublicCategoryItem(10L, "Gürültü"),
                new PublicCategoryItem(11L, "Çöp")
        ));

        mvc.perform(get("/api/public/departments/5/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Gürültü"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name").value("Çöp"));

        verify(service).activeCategoriesByDepartment(5L);
        verifyNoMoreInteractions(service);
    }
}
