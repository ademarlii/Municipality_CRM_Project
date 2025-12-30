package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.AdminDepartmentController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.admin.DepartmentResponse;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AdminDepartmentService;
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

@WebMvcTest(controllers = AdminDepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminDepartmentControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminDepartmentService service;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    private DepartmentResponse resp(long id, String name, boolean active) {
        DepartmentResponse r = new DepartmentResponse();
        r.setId(id);
        r.setName(name);
        r.setActive(active);
        return r;
    }

    @Test
    void create_ok_shouldReturn200_andBody() throws Exception {
        when(service.create(any(DepartmentUpsertRequest.class)))
                .thenReturn(resp(1, "Zabıta", true));

        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Zabıta","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Zabıta"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).create(any(DepartmentUpsertRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void create_invalid_blankName_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_emptyJson_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    void create_invalidJson_should400_invalidJson() throws Exception {
        mvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        // ✅ bilerek bozuk JSON
                        .content("{\"name\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(service);
    }

    @Test
    void list_ok_shouldReturnPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<DepartmentResponse> page = new PageImpl<>(
                List.of(resp(1, "A", true)),
                pageable,
                1
        );

        when(service.list(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/admin/departments?page=0&size=10&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A"))
                .andExpect(jsonPath("$.content[0].active").value(true));

        verify(service).list(any(Pageable.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void update_ok_shouldReturn200() throws Exception {
        when(service.update(eq(10L), any(DepartmentUpsertRequest.class)))
                .thenReturn(resp(10, "Yeni", false));

        mvc.perform(put("/api/admin/departments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yeni","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Yeni"))
                .andExpect(jsonPath("$.active").value(false));

        verify(service).update(eq(10L), any(DepartmentUpsertRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void update_invalid_blankName_should400_validationFailed() throws Exception {
        mvc.perform(put("/api/admin/departments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    void delete_ok_shouldReturn200() throws Exception {
        mvc.perform(delete("/api/admin/departments/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).delete(10L);
        verifyNoMoreInteractions(service);
    }
}
