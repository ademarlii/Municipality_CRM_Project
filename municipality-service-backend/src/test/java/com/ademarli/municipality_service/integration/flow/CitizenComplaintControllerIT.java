package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.complaint.CreateComplaintRequest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.Notification;
import com.ademarli.municipality_service.model.entity.StatusHistory;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.NotificationRepository;
import com.ademarli.municipality_service.repository.StatusHistoryRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CitizenComplaintControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired ComplaintCategoryRepository categoryRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired StatusHistoryRepository statusHistoryRepository;
    @Autowired NotificationRepository notificationRepository;

    private String citizenToken;
    private Long citizenId;

    @BeforeEach
    void setup() throws Exception {
        notificationRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        complaintRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();


        User u = new User();
        u.setEmail("citizen@test.com");
        u.setPhone("5550000001");
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(Role.CITIZEN));
        u.setEnabled(true);
        userRepository.save(u);
        citizenId = u.getId();


        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone("citizen@test.com");
        lr.setPassword("Password123!");

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        citizenToken = om.readTree(loginJson).get("accessToken").asText();
        assertNotNull(citizenToken);
        assertFalse(citizenToken.isBlank());
    }

    private Department dept(String name, boolean active) {
        Department d = new Department();
        d.setName(name);
        d.setActive(active);
        return departmentRepository.save(d);
    }

    private ComplaintCategory cat(String name, boolean active, Department defaultDept) {
        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setActive(active);
        c.setDefaultDepartment(defaultDept);
        return categoryRepository.save(c);
    }

    private CreateComplaintRequest createReq(Long categoryId, String title, String desc, Double lat, Double lon) {
        CreateComplaintRequest r = new CreateComplaintRequest();
        r.setCategoryId(categoryId);
        r.setTitle(title);
        r.setDescription(desc);
        r.setLat(lat);
        r.setLon(lon);
        return r;
    }

    @Test
    void create_ok_should200_andPersistComplaint_StatusHistory_Notification() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", true, d);

        CreateComplaintRequest req = createReq(c.getId(), "  Sokakta gürültü  ", "  açıklama  ", 38.1, 27.2);

        String json = mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.trackingCode").isString())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.title").value("Sokakta gürültü"))
                .andExpect(jsonPath("$.description").value("açıklama"))
                .andExpect(jsonPath("$.categoryId").value(c.getId()))
                .andExpect(jsonPath("$.departmentId").value(d.getId()))
                .andReturn().getResponse().getContentAsString();

        long complaintId = om.readTree(json).get("id").asLong();

        Complaint saved = complaintRepository.findById(complaintId).orElseThrow();
        assertEquals(ComplaintStatus.NEW, saved.getStatus());
        assertEquals(citizenId, saved.getCreatedBy().getId());
        assertEquals(c.getId(), saved.getCategory().getId());
        assertEquals(d.getId(), saved.getDepartment().getId());
        assertNotNull(saved.getTrackingCode());
        assertTrue(saved.getTrackingCode().startsWith("TRK-"));

        List<StatusHistory> history = statusHistoryRepository.findAll();
        assertEquals(1, history.size());
        assertEquals(complaintId, history.get(0).getComplaint().getId());
        assertNull(history.get(0).getFromStatus());
        assertEquals(ComplaintStatus.NEW, history.get(0).getToStatus());

        List<Notification> notifs = notificationRepository.findAll();
        assertEquals(1, notifs.size());
        assertEquals(citizenId, notifs.get(0).getUserId());
        assertEquals(complaintId, notifs.get(0).getComplaintId());
    }

    @Test
    void create_validation_should400_VALIDATION_FAILED() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", true, d);

        CreateComplaintRequest req = createReq(c.getId(), "   ", "x", null, null); // title @NotBlank patlar

        mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void create_categoryNotFound_should404_CATEGORY_NOT_FOUND() throws Exception {
        CreateComplaintRequest req = createReq(999L, "Test", "desc", null, null);

        mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void create_categoryNotActive_should409_CATEGORY_NOT_ACTIVE() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", false, d);

        CreateComplaintRequest req = createReq(c.getId(), "Test", "desc", null, null);

        mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_ACTIVE"));
    }

    @Test
    void create_defaultDepartmentNotActive_should409_DEFAULT_DEPARTMENT_NOT_ACTIVE() throws Exception {
        Department d = dept("Zabıta", false);
        ComplaintCategory c = cat("Gürültü", true, d);

        CreateComplaintRequest req = createReq(c.getId(), "Test", "desc", null, null);

        mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEFAULT_DEPARTMENT_NOT_ACTIVE"));
    }


    @Test
    void myComplaints_ok_should200_andReturnOnlyMyItems() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", true, d);

        CreateComplaintRequest r1 = createReq(c.getId(), "A", "d", null, null);
        CreateComplaintRequest r2 = createReq(c.getId(), "B", "d", null, null);

        mvc.perform(post("/api/citizen/complaints")
                .header("Authorization", "Bearer " + citizenToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r1))).andExpect(status().isOk());

        mvc.perform(post("/api/citizen/complaints")
                .header("Authorization", "Bearer " + citizenToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r2))).andExpect(status().isOk());

        mvc.perform(get("/api/citizen/complaints/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].trackingCode").exists())
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void getOne_ok_should200() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", true, d);

        String createdJson = mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(createReq(c.getId(), "Başlık", "desc", null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long complaintId = om.readTree(createdJson).get("id").asLong();

        mvc.perform(get("/api/citizen/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaintId))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.categoryId").value(c.getId()))
                .andExpect(jsonPath("$.departmentId").value(d.getId()));
    }

    @Test
    void getOne_notOwner_should403() throws Exception {
        User other = new User();
        other.setEmail("other@test.com");
        other.setPhone("5550000002");
        other.setPasswordHash(passwordEncoder.encode("Password123!"));
        other.setRoles(Set.of(Role.CITIZEN));
        other.setEnabled(true);
        userRepository.save(other);

        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone("other@test.com");
        lr.setPassword("Password123!");

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String otherToken = om.readTree(loginJson).get("accessToken").asText();

        Department d = dept("Zabıta", true);
        ComplaintCategory c = cat("Gürültü", true, d);

        String createdJson = mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(createReq(c.getId(), "Başlık", "desc", null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long complaintId = om.readTree(createdJson).get("id").asLong();

        mvc.perform(get("/api/citizen/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

    }

    @Test
    void getOne_notFound_should404_COMPLAINT_NOT_FOUND() throws Exception {
        mvc.perform(get("/api/citizen/complaints/999999")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPLAINT_NOT_FOUND"));
    }
}
