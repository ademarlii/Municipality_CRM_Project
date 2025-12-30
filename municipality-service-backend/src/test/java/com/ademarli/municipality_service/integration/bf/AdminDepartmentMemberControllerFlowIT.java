package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminDepartmentMemberControllerFlowIT extends BaseIntegrationTest {

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

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        Optional<User> us = userRepository.findByEmail("admin@test.com");

        if (us.isEmpty()) {
            User u = new User();
            u.setEmail("admin@test.com");
            u.setPhone("5550000000");
            u.setPasswordHash(passwordEncoder.encode("Password123!"));
            u.setRoles(Set.of(Role.ADMIN));
            userRepository.save(u);
        }

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"admin@test.com","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = om.readTree(loginJson).get("accessToken").asText();
    }

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
        departmentMemberRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void add_list_changeRole_remove_flow_withJwt() throws Exception {
        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"BF Dept Members","active":true}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();


        User agent = new User();
        agent.setEmail("agent1@test.com");
        agent.setPhone("5540000001");
        agent.setPasswordHash(passwordEncoder.encode("Password123!"));
        agent.setRoles(Set.of(Role.AGENT));
        userRepository.save(agent);

        long userId = agent.getId();

        com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest adm = new com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest();
        adm.setUserId(userId);
        adm.setMemberRole("MEMBER");

        mvc.perform(post("/api/admin/departments/" + deptId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(adm)))
                .andExpect(status().isOk());


        mvc.perform(get("/api/admin/departments/" + deptId + "/members")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].memberRole").value("MEMBER"))
                .andExpect(jsonPath("$[0].active").value(true));


        mvc.perform(patch("/api/admin/departments/" + deptId + "/members/" + userId + "/role/MANAGER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());


        mvc.perform(get("/api/admin/departments/" + deptId + "/members")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberRole").value("MANAGER"));


        mvc.perform(delete("/api/admin/departments/" + deptId + "/members/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/departments/" + deptId + "/members")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false));
    }
}
