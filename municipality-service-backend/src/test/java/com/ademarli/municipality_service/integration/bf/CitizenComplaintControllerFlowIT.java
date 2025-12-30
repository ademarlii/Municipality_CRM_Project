package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CitizenComplaintControllerFlowIT extends BaseIntegrationTest {

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

    private String adminToken;

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


        com.ademarli.municipality_service.model.dto.auth.LoginRequest lreq = new com.ademarli.municipality_service.model.dto.auth.LoginRequest();
        lreq.setEmailOrPhone("admin@test.com");
        lreq.setPassword("Password123!");

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lreq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = om.readTree(loginJson).get("accessToken").asText();
    }

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_and_list_and_getComplaint_asCitizen() throws Exception {
        // create department + category
        DepartmentUpsertRequest dreq = new DepartmentUpsertRequest();
        dreq.setName("CitizenDept");
        dreq.setActive(true);

        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dreq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();

        CategoryUpsertRequest creq = new CategoryUpsertRequest();
        creq.setName("CitizenCat");
        creq.setDefaultDepartmentId(deptId);
        creq.setActive(true);

        String catJson = mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(creq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long catId = om.readTree(catJson).get("id").asLong();

        // register citizen and get token
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("citizen2@test.com");
        reg.setPhone("5553000003");
        reg.setPassword("Password123!");

        String regJson = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String citizenToken = om.readTree(regJson).get("accessToken").asText();

        // create complaint as citizen
        com.ademarli.municipality_service.model.dto.complaint.CreateComplaintRequest creqDto = new com.ademarli.municipality_service.model.dto.complaint.CreateComplaintRequest();
        creqDto.setTitle("My comp");
        creqDto.setDescription("x");
        creqDto.setCategoryId(catId);

        String complaintJson = mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(creqDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My comp"))
                .andReturn().getResponse().getContentAsString();

        long complaintId = om.readTree(complaintJson).get("id").asLong();

        // list my complaints
        mvc.perform(get("/api/citizen/complaints/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(complaintId));

        // get one
        mvc.perform(get("/api/citizen/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaintId));
    }
}
