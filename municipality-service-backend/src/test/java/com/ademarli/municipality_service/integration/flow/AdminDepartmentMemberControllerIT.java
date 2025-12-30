package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.DepartmentMember;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.DepartmentMemberRole;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.*;
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

class AdminDepartmentMemberControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired DepartmentMemberRepository departmentMemberRepository;
    @Autowired
    ComplaintRepository complaintRepository;
    @Autowired
    ComplaintCategoryRepository complaintCategoryRepository;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        departmentMemberRepository.deleteAll();
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

    private Department dept(String name, boolean active) {
        Department d = new Department();
        d.setName(name);
        d.setActive(active);
        return departmentRepository.save(d);
    }

    private User agentUser(String email,String phone) {
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(Role.AGENT));
        return userRepository.save(u);
    }

    private User citizenUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPhone("5559998877");
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(Role.CITIZEN));
        return userRepository.save(u);
    }

    private AddDepartmentMemberRequest addReq(Long userId, String memberRole) {
        AddDepartmentMemberRequest r = new AddDepartmentMemberRequest();
        r.setUserId(userId);
        r.setMemberRole(memberRole);
        return r;
    }

    private String auth() {
        return "Bearer " + adminToken;
    }

    @Test
    void add_ok_roleNull_shouldDefaultMember_andPersist() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent@test.com", "5551112222");

        AddDepartmentMemberRequest req = addReq(agent.getId(), null);

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        DepartmentMember m = departmentMemberRepository
                .findByDepartmentIdAndUserId(d.getId(), agent.getId())
                .orElseThrow();

        assertTrue(m.isActive());
        assertEquals(DepartmentMemberRole.MEMBER, m.getMemberRole());
        assertEquals(d.getId(), m.getDepartment().getId());
        assertEquals(agent.getId(), m.getUser().getId());
    }

    @Test
    void add_ok_roleManager_shouldPersistManager() throws Exception {
        Department d = dept("Temizlik", true);
        User agent = agentUser("agent2@test.com","5551112222");

        AddDepartmentMemberRequest req = addReq(agent.getId(), "MANAGER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        DepartmentMember m = departmentMemberRepository
                .findByDepartmentIdAndUserId(d.getId(), agent.getId())
                .orElseThrow();

        assertTrue(m.isActive());
        assertEquals(DepartmentMemberRole.MANAGER, m.getMemberRole());
    }

    @Test
    void add_existingMember_shouldReactivate_andUpdateRole() throws Exception {
        Department d = dept("Fen", true);
        User agent = agentUser("agent3@test.com","5551112222");

        DepartmentMember existing = new DepartmentMember();
        existing.setDepartment(d);
        existing.setUser(agent);
        existing.setMemberRole(DepartmentMemberRole.MEMBER);
        existing.setActive(false);
        departmentMemberRepository.save(existing);

        AddDepartmentMemberRequest req = addReq(agent.getId(), "MANAGER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        DepartmentMember m = departmentMemberRepository
                .findByDepartmentIdAndUserId(d.getId(), agent.getId())
                .orElseThrow();

        assertTrue(m.isActive());
        assertEquals(DepartmentMemberRole.MANAGER, m.getMemberRole());
    }


    @Test
    void list_ok_shouldReturnMembers() throws Exception {
        Department d = dept("Zabıta", true);
        User a1 = agentUser("a1@test.com","5551112222");
        User a2 = agentUser("a2@test.com","5551112223");

        DepartmentMember m1 = new DepartmentMember();
        m1.setDepartment(d);
        m1.setUser(a1);
        m1.setMemberRole(DepartmentMemberRole.MEMBER);
        m1.setActive(true);

        DepartmentMember m2 = new DepartmentMember();
        m2.setDepartment(d);
        m2.setUser(a2);
        m2.setMemberRole(DepartmentMemberRole.MANAGER);
        m2.setActive(false);

        departmentMemberRepository.save(m1);
        departmentMemberRepository.save(m2);

        mvc.perform(get("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].memberRole").exists())
                .andExpect(jsonPath("$[0].active").exists());
    }

    @Test
    void remove_ok_shouldSetActiveFalse() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent-remove@test.com","5551112222");

        DepartmentMember m = new DepartmentMember();
        m.setDepartment(d);
        m.setUser(agent);
        m.setMemberRole(DepartmentMemberRole.MEMBER);
        m.setActive(true);
        departmentMemberRepository.save(m);

        mvc.perform(delete("/api/admin/departments/{deptId}/members/{userId}", d.getId(), agent.getId())
                        .header("Authorization", auth()))
                .andExpect(status().isOk());

        DepartmentMember after = departmentMemberRepository
                .findByDepartmentIdAndUserId(d.getId(), agent.getId())
                .orElseThrow();

        assertFalse(after.isActive());
    }

    @Test
    void remove_notFound_should404() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent-nf@test.com","5551112222");

        mvc.perform(delete("/api/admin/departments/{deptId}/members/{userId}", d.getId(), agent.getId())
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_MEMBER_NOT_FOUND"));
    }

    @Test
    void changeRole_ok_shouldUpdateRole() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent-role@test.com","5551112222");

        DepartmentMember m = new DepartmentMember();
        m.setDepartment(d);
        m.setUser(agent);
        m.setMemberRole(DepartmentMemberRole.MEMBER);
        m.setActive(true);
        departmentMemberRepository.save(m);

        mvc.perform(patch("/api/admin/departments/{deptId}/members/{userId}/role/{role}", d.getId(), agent.getId(), "MANAGER")
                        .header("Authorization", auth()))
                .andExpect(status().isOk());

        DepartmentMember after = departmentMemberRepository
                .findByDepartmentIdAndUserId(d.getId(), agent.getId())
                .orElseThrow();

        assertEquals(DepartmentMemberRole.MANAGER, after.getMemberRole());
    }

    @Test
    void changeRole_notFound_should404() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent-role-nf@test.com","5551112222");

        mvc.perform(patch("/api/admin/departments/{deptId}/members/{userId}/role/{role}", d.getId(), agent.getId(), "MANAGER")
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_MEMBER_NOT_FOUND"));
    }

    @Test
    void add_deptNotFound_should404() throws Exception {
        User agent = agentUser("agent-deptnf@test.com","5551112222");
        AddDepartmentMemberRequest req = addReq(agent.getId(), "MEMBER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", 999999L)
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void add_deptNotActive_should409() throws Exception {
        Department d = dept("Pasif Dept", false);
        User agent = agentUser("agent-passive@test.com","5551112222");

        AddDepartmentMemberRequest req = addReq(agent.getId(), "MEMBER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_ACTIVE"));
    }

    @Test
    void add_userNotFound_should404() throws Exception {
        Department d = dept("Zabıta", true);

        AddDepartmentMemberRequest req = addReq(999999L, "MEMBER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void add_onlyAgent_should409_whenUserIsCitizen() throws Exception {
        Department d = dept("Zabıta", true);
        User citizen = citizenUser("citizen@test.com");

        AddDepartmentMemberRequest req = addReq(citizen.getId(), "MEMBER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONLY_AGENT_CAN_BE_DEPARTMENT_MEMBER"));
    }

    @Test
    void add_invalidMemberRole_should409() throws Exception {
        Department d = dept("Zabıta", true);
        User agent = agentUser("agent-badrole@test.com","5551112222");

        AddDepartmentMemberRequest req = addReq(agent.getId(), "BOSS"); // invalid

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_ROLE"));
    }

    @Test
    void add_validation_missingUserId_should400() throws Exception {
        Department d = dept("Zabıta", true);

        AddDepartmentMemberRequest req = addReq(null, "MEMBER");

        mvc.perform(post("/api/admin/departments/{deptId}/members", d.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.userId").exists());
    }
}
