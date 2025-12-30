package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicCatalogControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired
    ComplaintRepository complaintRepository;

    private String adminToken;

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setupAdmin() throws Exception {
        var opt = userRepository.findByEmail("admin@test.com");
        if (opt.isEmpty()) {
            User u = new User();
            u.setEmail("admin@test.com");
            u.setPhone("5550000000");
            u.setPasswordHash(passwordEncoder.encode("Password123!"));
            u.setRoles(Set.of(Role.ADMIN));
            userRepository.save(u);
        }

        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone("admin@test.com");
        lr.setPassword("Password123!");

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = om.readTree(loginJson).get("accessToken").asText();
    }

    @Test
    void public_departments_and_categories() throws Exception {
        DepartmentUpsertRequest dreq = new DepartmentUpsertRequest();
        dreq.setName("PubDept");
        dreq.setActive(true);

        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dreq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();

        com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest catReq = new com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest();
        catReq.setName("PubCat");
        catReq.setDefaultDepartmentId(deptId);
        catReq.setActive(true);

        mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(catReq)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        mvc.perform(get("/api/public/departments/" + deptId + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
