package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.AdminCategoryController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.admin.CategoryResponse;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AdminCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminCategoryControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminCategoryService service;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    private CategoryResponse resp(long id, String name, boolean active, long deptId, String deptName) {
        CategoryResponse r = new CategoryResponse();
        r.setId(id);
        r.setName(name);
        r.setActive(active);
        r.setDefaultDepartmentId(deptId);
        r.setDefaultDepartmentName(deptName);
        return r;
    }

    @Test
    void create_ok_shouldReturn200_andBody() throws Exception {
        when(service.create(any(CategoryUpsertRequest.class)))
                .thenReturn(resp(1, "Gürültü", true, 5, "Zabıta"));

        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Gürültü","defaultDepartmentId":5,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Gürültü"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.defaultDepartmentId").value(5))
                .andExpect(jsonPath("$.defaultDepartmentName").value("Zabıta"));

        verify(service).create(any(CategoryUpsertRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void create_invalid_blankName_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","defaultDepartmentId":5,"active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_invalid_missingDefaultDepartmentId_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.defaultDepartmentId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_emptyJson_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.defaultDepartmentId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_invalidJson_should400_invalidJson() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(service);
    }

    @Test
    void list_ok_shouldReturnPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<CategoryResponse> page = new PageImpl<>(
                List.of(resp(1, "A", true, 5, "Zabıta")),
                pageable,
                1
        );

        when(service.list(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/admin/categories?page=0&size=10&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A"));

        verify(service).list(any(Pageable.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void update_ok_shouldReturn200() throws Exception {
        when(service.update(eq(10L), any(CategoryUpsertRequest.class)))
                .thenReturn(resp(10, "Yeni", false, 2, "Temizlik"));

        mvc.perform(put("/api/admin/categories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yeni","defaultDepartmentId":2,"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Yeni"))
                .andExpect(jsonPath("$.active").value(false));

        verify(service).update(eq(10L), any(CategoryUpsertRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void delete_ok_shouldReturn200() throws Exception {
        mvc.perform(delete("/api/admin/categories/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).delete(10L);
        verifyNoMoreInteractions(service);
    }
}

