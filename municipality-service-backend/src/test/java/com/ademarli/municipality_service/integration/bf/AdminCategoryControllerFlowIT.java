package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.NotificationRepository;
import com.ademarli.municipality_service.repository.FeedbackRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCategoryControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ComplaintRepository complaintRepository;
    @Autowired
    ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    DepartmentMemberRepository departmentMemberRepository;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    FeedbackRepository feedbackRepository;

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

        userRepository.findByEmail("admin@test.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin@test.com");
            u.setPhone("5550000000");
            u.setPasswordHash(passwordEncoder.encode("Password123!"));
            u.setRoles(Set.of(Role.ADMIN));
            return userRepository.save(u);
        });

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
    void create_list_update_delete_flow_withJwt() throws Exception {

        DepartmentUpsertRequest dreq = new DepartmentUpsertRequest();
        dreq.setName("BF Yönetim");
        dreq.setActive(true);

        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dreq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();

        CategoryUpsertRequest creq = new CategoryUpsertRequest();
        creq.setName("BF Kategori");
        creq.setDefaultDepartmentId(deptId);
        creq.setActive(true);

        String catJson = mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(creq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("BF Kategori"))
                .andReturn().getResponse().getContentAsString();

        long catId = om.readTree(catJson).get("id").asLong();

        mvc.perform(get("/api/admin/categories?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").isArray())
                .andExpect(jsonPath("$.content[0].name").exists());

        // update using DTO
        CategoryUpsertRequest upd = new CategoryUpsertRequest();
        upd.setName("BF Kategori Güncel");
        upd.setDefaultDepartmentId(deptId);
        upd.setActive(false);

        mvc.perform(put("/api/admin/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(catId))
                .andExpect(jsonPath("$.name").value("BF Kategori Güncel"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete("/api/admin/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/categories?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)].active".formatted(catId)).value(false));
    }
}
