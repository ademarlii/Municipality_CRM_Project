package com.ademarli.municipality_service.integration.full;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCategoryAndDepartmentFlowIT extends BaseIntegrationTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;

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

    @Test
    void scenario_adminCreateCategory_fullJwt() throws Exception {
        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yönetim","active":true}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();

        String catJson = mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yönetim kontrol","defaultDepartmentId":%d,"active":true}
                                """.formatted(deptId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long catId = om.readTree(catJson).get("id").asLong();

        mvc.perform(get("/api/admin/categories?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Gürültü-2","defaultDepartmentId":%d,"active":false}
                                """.formatted(deptId)))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

}
