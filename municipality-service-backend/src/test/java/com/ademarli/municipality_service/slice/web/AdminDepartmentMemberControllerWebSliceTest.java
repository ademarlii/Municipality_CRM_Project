package com.ademarli.municipality_service.slice.web;

import com.ademarli.municipality_service.controller.AdminDepartmentMemberController;
import com.ademarli.municipality_service.exception.GlobalExceptionHandler;
import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentMemberResponse;
import com.ademarli.municipality_service.security.CustomUserDetailsService;
import com.ademarli.municipality_service.security.JwtFilter;
import com.ademarli.municipality_service.security.RestAccessDeniedHandler;
import com.ademarli.municipality_service.security.RestAuthEntryPoint;
import com.ademarli.municipality_service.service.AdminDepartmentMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminDepartmentMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminDepartmentMemberControllerWebSliceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminDepartmentMemberService service;

    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean RestAuthEntryPoint restAuthEntryPoint;
    @MockitoBean RestAccessDeniedHandler restAccessDeniedHandler;

    private DepartmentMemberResponse member(long userId, String email, String phone, String role, boolean active) {
        DepartmentMemberResponse r = new DepartmentMemberResponse();
        r.setUserId(userId);
        r.setEmail(email);
        r.setPhone(phone);
        r.setMemberRole(role);
        r.setActive(active);
        return r;
    }


    @Test
    void add_ok_shouldReturn200() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"memberRole":"MEMBER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).addMember(eq(10L), any(AddDepartmentMemberRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void add_invalid_missingUserId_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberRole":"MEMBER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.userId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void add_emptyJson_should400_validationFailed() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.userId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void add_invalidJson_should400_invalidJson() throws Exception {
        mvc.perform(post("/api/admin/departments/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(service);
    }


    @Test
    void list_ok_shouldReturn200_andList() throws Exception {
        when(service.listMembers(10L)).thenReturn(List.of(
                member(5, "a@b.com", "0555", "MEMBER", true),
                member(7, "c@d.com", "0533", "MANAGER", false)
        ));

        mvc.perform(get("/api/admin/departments/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(5))
                .andExpect(jsonPath("$[0].email").value("a@b.com"))
                .andExpect(jsonPath("$[0].phone").value("0555"))
                .andExpect(jsonPath("$[0].memberRole").value("MEMBER"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].userId").value(7))
                .andExpect(jsonPath("$[1].memberRole").value("MANAGER"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(service).listMembers(10L);
        verifyNoMoreInteractions(service);
    }


    @Test
    void remove_ok_shouldReturn200() throws Exception {
        mvc.perform(delete("/api/admin/departments/10/members/99"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).removeMember(10L, 99L);
        verifyNoMoreInteractions(service);
    }


    @Test
    void changeRole_ok_shouldReturn200() throws Exception {
        mvc.perform(patch("/api/admin/departments/10/members/99/role/MANAGER"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).changeRole(10L, 99L, "MANAGER");
        verifyNoMoreInteractions(service);
    }
}
