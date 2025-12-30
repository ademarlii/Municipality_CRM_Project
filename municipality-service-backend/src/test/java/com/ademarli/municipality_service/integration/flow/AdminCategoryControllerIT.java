package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.admin.CategoryResponse;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.FeedbackRepository;
import com.ademarli.municipality_service.repository.NotificationRepository;
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

class AdminCategoryControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired DepartmentMemberRepository departmentMemberRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired FeedbackRepository feedbackRepository;

    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
        departmentMemberRepository.deleteAll();
        departmentRepository.deleteAll();
        notificationRepository.deleteAll();
        feedbackRepository.deleteAll();
        userRepository.deleteAll();

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

    private Department createDeptEntity(String name, boolean active) {
        Department d = new Department();
        d.setName(name);
        d.setActive(active);
        return departmentRepository.save(d);
    }

    private ComplaintCategory createCategoryEntity(String name, Department dept, boolean active) {
        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setDefaultDepartment(dept);
        c.setActive(active);
        return complaintCategoryRepository.save(c);
    }

    private CategoryUpsertRequest catReq(String name, Long deptId, Boolean active) {
        CategoryUpsertRequest r = new CategoryUpsertRequest();
        r.setName(name);
        r.setDefaultDepartmentId(deptId);
        r.setActive(active);
        return r;
    }

    @Test
    void create_list_update_delete_flow() throws Exception {
        Department dept = createDeptEntity("Zabıta", true);

        CategoryUpsertRequest creq = catReq("BF Kategori", dept.getId(), true);

        MvcResult createRes = mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(creq)))
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse created = om.readValue(createRes.getResponse().getContentAsString(), CategoryResponse.class);
        assertNotNull(created.getId());
        assertEquals("BF Kategori", created.getName());
        assertTrue(created.isActive());
        assertEquals(dept.getId(), created.getDefaultDepartmentId());
        assertEquals("Zabıta", created.getDefaultDepartmentName());

        Long catId = created.getId();

        // LIST
        mvc.perform(get("/api/admin/categories?page=0&size=10&sort=name,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());

        // UPDATE
        CategoryUpsertRequest upd = catReq("BF Kategori Güncel", dept.getId(), false);

        MvcResult updRes = mvc.perform(put("/api/admin/categories/{id}", catId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse updated = om.readValue(updRes.getResponse().getContentAsString(), CategoryResponse.class);
        assertEquals(catId, updated.getId());
        assertEquals("BF Kategori Güncel", updated.getName());
        assertFalse(updated.isActive());

        mvc.perform(delete("/api/admin/categories/{id}", catId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        ComplaintCategory deleted = complaintCategoryRepository.findById(catId).orElseThrow();
        assertFalse(deleted.isActive());
    }


    @Test
    void create_duplicateName_should400() throws Exception {
        Department dept = createDeptEntity("Zabıta", true);
        createCategoryEntity("Gürültü", dept, true);

        CategoryUpsertRequest req = catReq("gürültü", dept.getId(), true); // case-insensitive çakışma

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_ALREADY_EXISTS"));
    }

    @Test
    void create_defaultDepartmentNotFound_should404() throws Exception {
        CategoryUpsertRequest req = catReq("Yeni", 999999L, true);

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEFAULT_DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void create_departmentNotActive_should400() throws Exception {
        Department dept = createDeptEntity("Temizlik", false);

        CategoryUpsertRequest req = catReq("Yeni", dept.getId(), true);

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_ACTIVE"));
    }

    @Test
    void update_categoryNotFound_should404() throws Exception {
        Department dept = createDeptEntity("Zabıta", true);

        CategoryUpsertRequest req = catReq("Yeni", dept.getId(), true);

        mvc.perform(put("/api/admin/categories/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void update_duplicateName_should400() throws Exception {
        Department dept = createDeptEntity("Zabıta", true);

        ComplaintCategory c1 = createCategoryEntity("A", dept, true);
        createCategoryEntity("B", dept, true);

        CategoryUpsertRequest req = catReq("b", dept.getId(), true);

        mvc.perform(put("/api/admin/categories/{id}", c1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_ALREADY_EXISTS"));
    }


    @Test
    void create_invalid_blankName_should400_validationFailed() throws Exception {
        Department dept = createDeptEntity("Zabıta", true);

        CategoryUpsertRequest req = catReq("   ", dept.getId(), true);

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_invalid_missingDefaultDepartmentId_should400_validationFailed() throws Exception {
        CategoryUpsertRequest req = new CategoryUpsertRequest();
        req.setName("A");
        req.setDefaultDepartmentId(null);
        req.setActive(true);

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.defaultDepartmentId").exists());
    }
}
