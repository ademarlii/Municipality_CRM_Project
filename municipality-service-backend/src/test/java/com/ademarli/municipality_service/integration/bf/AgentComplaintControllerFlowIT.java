package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.complaint.ChangeStatusRequest;
import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AgentComplaintControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ComplaintRepository complaintRepository;

    @Autowired
    ComplaintCategoryRepository categoryRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    DepartmentMemberRepository departmentMemberRepository;

    @Autowired
    UserRepository userRepository;

    private String adminToken;

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentMemberRepository.deleteAll();
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
    void agent_list_and_changeStatus_flow() throws Exception {
        DepartmentUpsertRequest dreq = new DepartmentUpsertRequest();
        dreq.setName("AgentDept");
        dreq.setActive(true);

        String deptJson = mvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dreq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(deptJson).get("id").asLong();

        CategoryUpsertRequest catReq = new CategoryUpsertRequest();
        catReq.setName("AgentCat");
        catReq.setDefaultDepartmentId(deptId);
        catReq.setActive(true);

        String catJson = mvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(catReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long catId = om.readTree(catJson).get("id").asLong();

        User citizen = new User();
        citizen.setEmail("citizen1@test.com");
        citizen.setPhone("5551000001");
        citizen.setPasswordHash(passwordEncoder.encode("Password123!"));
        citizen.setRoles(Set.of(Role.CITIZEN));
        userRepository.save(citizen);

        Complaint c = new Complaint();
        c.setTrackingCode("TRK-AG-1");
        c.setTitle("Test complaint");
        c.setDescription("desc");
        c.setCreatedBy(citizen);
        c.setDepartment(departmentRepository.findById(deptId).orElseThrow());
        c.setCategory(categoryRepository.findById(catId).orElseThrow());
        c.setStatus(ComplaintStatus.NEW);
        c.setCreatedAt(OffsetDateTime.now());
        complaintRepository.save(c);

        long complaintId = c.getId();

        User agent = new User();
        agent.setEmail("agent2@test.com");
        agent.setPhone("5552000002");
        agent.setPasswordHash(passwordEncoder.encode("Password123!"));
        agent.setRoles(Set.of(Role.AGENT));
        userRepository.save(agent);

        AddDepartmentMemberRequest adm = new AddDepartmentMemberRequest();
        adm.setUserId(agent.getId());
        adm.setMemberRole("MEMBER");

        mvc.perform(post("/api/admin/departments/" + deptId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(adm)))
                .andExpect(status().isOk());

        LoginRequest agentLoginReq = new LoginRequest();
        agentLoginReq.setEmailOrPhone(agent.getEmail());
        agentLoginReq.setPassword("Password123!");

        String agentLogin = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(agentLoginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String agentToken = om.readTree(agentLogin).get("accessToken").asText();


        mvc.perform(get("/api/agent/complaints?page=0&size=10&sort=createdAt,desc")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(complaintId))
                .andExpect(jsonPath("$.content[0].trackingCode").value("TRK-AG-1"));


        ChangeStatusRequest csr = new ChangeStatusRequest();
        csr.setToStatus("IN_REVIEW");


        mvc.perform(post("/api/agent/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(csr)))
                .andExpect(status().isOk());


        ChangeStatusRequest csr2 = new ChangeStatusRequest();
        csr2.setToStatus("RESOLVED");
        csr2.setNote("ok");
        csr2.setPublicAnswer("done");

        mvc.perform(post("/api/agent/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(csr2)))
                .andExpect(status().isOk());

    }
}
