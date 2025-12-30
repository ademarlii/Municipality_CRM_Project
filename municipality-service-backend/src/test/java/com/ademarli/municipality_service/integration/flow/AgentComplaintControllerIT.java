package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.complaint.ChangeStatusRequest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.DepartmentMember;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.DepartmentMemberRole;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AgentComplaintControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired DepartmentMemberRepository departmentMemberRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired ComplaintRepository complaintRepository;

    @BeforeEach
    void setup() {
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
        departmentMemberRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User mkUser(String email, String phone, String rawPass, Role... roles) {
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(rawPass));
        u.setRoles(Set.of(roles));
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private String loginToken(String emailOrPhone, String password) throws Exception {
        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone(emailOrPhone);
        lr.setPassword(password);

        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return om.readTree(json).get("accessToken").asText();
    }

    private Department mkDept(String name, boolean active) {
        Department d = new Department();
        d.setName(name);
        d.setActive(active);
        return departmentRepository.save(d);
    }

    private void addMembership(User agent, Department dept, DepartmentMemberRole role, boolean active) {
        DepartmentMember m = new DepartmentMember();
        m.setUser(agent);
        m.setDepartment(dept);
        m.setMemberRole(role);
        m.setActive(active);
        departmentMemberRepository.save(m);
    }

    private ComplaintCategory mkCategory(String name, boolean active, Department defaultDept) {
        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setActive(active);
        c.setDefaultDepartment(defaultDept);
        return complaintCategoryRepository.save(c);
    }

    private Complaint mkComplaint(String tracking, String title, ComplaintStatus status,
                                  Department dept, ComplaintCategory cat, User citizen) {
        Complaint c = new Complaint();
        c.setTrackingCode(tracking);
        c.setTitle(title);
        c.setStatus(status);
        c.setCreatedAt(OffsetDateTime.now());
        c.setDepartment(dept);
        c.setCategory(cat);
        c.setCreatedBy(citizen);
        return complaintRepository.save(c);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ChangeStatusRequest changeReq(String toStatus, String note, String publicAnswer) {
        ChangeStatusRequest r = new ChangeStatusRequest();
        r.setToStatus(toStatus);
        r.setNote(note);
        r.setPublicAnswer(publicAnswer);
        return r;
    }


    @Test
    void list_ok_shouldReturnOnlyComplaintsInAgentsDepartments_andFilterByStatusAndQuery() throws Exception {
        User agent = mkUser("agent@test.com", "5550000001", "Password123!", Role.AGENT);
        String token = loginToken("agent@test.com", "Password123!");

        Department d1 = mkDept("Zabıta", true);
        Department d2 = mkDept("Temizlik", true);

        addMembership(agent, d1, DepartmentMemberRole.MEMBER, true);

        User citizen = mkUser("citizen@test.com", "5550000002", "Password123!", Role.CITIZEN);

        ComplaintCategory cat1 = mkCategory("Gürültü", true, d1);
        ComplaintCategory cat2 = mkCategory("Çöp", true, d2);

        Complaint c1 = mkComplaint("TRK-1", "Çöp konteyneri dolu", ComplaintStatus.NEW, d1, cat1, citizen);
        Complaint c2 = mkComplaint("TRK-2", "Gürültü var", ComplaintStatus.RESOLVED, d1, cat1, citizen);
        mkComplaint("TRK-3", "Başka dept şikayeti", ComplaintStatus.NEW, d2, cat2, citizen); // gelmemeli

        mvc.perform(get("/api/agent/complaints?page=0&size=10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.trackingCode=='TRK-1')]").exists())
                .andExpect(jsonPath("$.content[?(@.trackingCode=='TRK-2')]").exists());

        mvc.perform(get("/api/agent/complaints?page=0&size=10&status=NEW")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(c1.getId()))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));

        mvc.perform(get("/api/agent/complaints?page=0&size=10&q=gürültü")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(c2.getId()));
    }

    @Test
    void list_userDeletedAfterLogin_should404_USER_NOT_FOUND() throws Exception {
        User agent = mkUser("agent@test.com", "5550000001", "Password123!", Role.AGENT);
        String token = loginToken("agent@test.com", "Password123!");

        userRepository.deleteById(agent.getId());

        mvc.perform(get("/api/agent/complaints?page=0&size=10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void list_notAgent_should409_ONLY_STAFF_CAN_CHANGE_STATUS() throws Exception {
        mkUser("citizen@test.com", "5550000002", "Password123!", Role.CITIZEN);
        String token = loginToken("citizen@test.com", "Password123!");

        mvc.perform(get("/api/agent/complaints?page=0&size=10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Access Denied"));
    }

    @Test
    void list_agentButNoMembership_should409_NOT_A_MEMBER_OF_THIS_DEPARTMENT() throws Exception {
        mkUser("agent@test.com", "5550000001", "Password123!", Role.AGENT);
        String token = loginToken("agent@test.com", "Password123!");
        mvc.perform(get("/api/agent/complaints?page=0&size=10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_A_MEMBER_OF_THIS_DEPARTMENT"));
    }


    @Test
    void changeStatus_ok_should200_andPersistStatus() throws Exception {
        User agent = mkUser("agent@test.com", "5550000001", "Password123!", Role.AGENT);
        String token = loginToken("agent@test.com", "Password123!");

        Department d1 = mkDept("Zabıta", true);
        addMembership(agent, d1, DepartmentMemberRole.MEMBER, true);

        User citizen = mkUser("citizen@test.com", "5550000002", "Password123!", Role.CITIZEN);
        ComplaintCategory cat1 = mkCategory("Gürültü", true, d1);

        Complaint c = mkComplaint("TRK-10", "Test", ComplaintStatus.NEW, d1, cat1, citizen);

        ChangeStatusRequest req = changeReq("IN_REVIEW", "inceleniyor", "Kayıt alındı");

        mvc.perform(post("/api/agent/complaints/{id}/status", c.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());


        Complaint after = complaintRepository.findById(c.getId()).orElseThrow();
        assertEquals(ComplaintStatus.NEW, after.getStatus());
    }

    @Test
    void changeStatus_blankToStatus_should400_VALIDATION_FAILED() throws Exception {
        User agent = mkUser("agent@test.com", "5550000001", "Password123!", Role.AGENT);
        String token = loginToken("agent@test.com", "Password123!");

        ChangeStatusRequest req = changeReq("   ", null, null);

        mvc.perform(post("/api/agent/complaints/{id}/status", 1L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.toStatus").exists());
    }
}
