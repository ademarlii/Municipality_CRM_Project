package com.ademarli.municipality_service.integration.flow;


import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentResponse;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminDepartmentControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired
    ComplaintRepository complaintRepository;
    @Autowired
    ComplaintCategoryRepository complaintCategoryRepository;

    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        complaintRepository.deleteAllInBatch();
        complaintCategoryRepository.deleteAllInBatch();

        departmentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();


        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPhone("5550000000");
        admin.setPasswordHash(passwordEncoder.encode("Password123!"));
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);

        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone("admin@test.com");
        lr.setPassword("Password123!");

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = om.readTree(loginJson).get("accessToken").asText();
        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());
    }

    private DepartmentUpsertRequest deptReq(String name, Boolean active) {
        DepartmentUpsertRequest r = new DepartmentUpsertRequest();
        r.setName(name);
        r.setActive(active);
        return r;
    }

    private Department createDeptEntity(String name, boolean active) {
        Department d = new Department();
        d.setName(name);
        d.setActive(active);
        return departmentRepository.save(d);
    }

    @Test
    void create_list_update_delete_flow() throws Exception {
        DepartmentUpsertRequest creq = deptReq("BF Yönetim", true);

        MvcResult createRes = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(creq)))
                .andExpect(status().isOk())
                .andReturn();

        DepartmentResponse created =
                om.readValue(createRes.getResponse().getContentAsString(), DepartmentResponse.class);

        assertNotNull(created.getId());
        assertEquals("BF Yönetim", created.getName());
        assertTrue(created.isActive());

        Long deptId = created.getId();

        mvc.perform(get("/api/admin/departments?page=0&size=10&sort=name,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());

        DepartmentUpsertRequest upd = deptReq("BF Yönetim Güncel", false);

        MvcResult updRes = mvc.perform(put("/api/admin/departments/{id}", deptId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andReturn();

        DepartmentResponse updated =
                om.readValue(updRes.getResponse().getContentAsString(), DepartmentResponse.class);

        assertEquals(deptId, updated.getId());
        assertEquals("BF Yönetim Güncel", updated.getName());
        assertFalse(updated.isActive());

        mvc.perform(delete("/api/admin/departments/{id}", deptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Department softDeleted = departmentRepository.findById(deptId).orElseThrow();
        assertFalse(softDeleted.isActive());
    }

    @Test
    void create_duplicateName_should409_conflict() throws Exception {
        createDeptEntity("Zabıta", true);

        DepartmentUpsertRequest req = deptReq("zabıta", true);

        mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NAME_ALREADY_EXISTS"));
    }

    @Test
    void update_duplicateName_should409_conflict() throws Exception {
        Department d1 = createDeptEntity("A", true);
        createDeptEntity("B", true);

        DepartmentUpsertRequest req = deptReq("b", true);

        mvc.perform(put("/api/admin/departments/{id}", d1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NAME_ALREADY_EXISTS"));
    }

    @Test
    void update_notFound_should404() throws Exception {
        DepartmentUpsertRequest req = deptReq("Yeni", true);

        mvc.perform(put("/api/admin/departments/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void delete_notFound_should404() throws Exception {
        mvc.perform(delete("/api/admin/departments/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void create_invalid_blankName_should400_validationFailed() throws Exception {
        DepartmentUpsertRequest req = deptReq("   ", true);

        mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}

