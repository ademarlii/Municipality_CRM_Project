package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.Notification;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.NotificationRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CitizenNotificationControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired DepartmentRepository departmentRepository;

    private String token1;
    private Long user1Id;

    private String token2;
    private Long user2Id;

    private Long deptId;
    private Long catId;

    @BeforeEach
    void setup() throws Exception {

        notificationRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        complaintCategoryRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User u1 = new User();
        u1.setEmail("citizen1@test.com");
        u1.setPhone("5550000101");
        u1.setPasswordHash(passwordEncoder.encode("Password123!"));
        u1.setRoles(Set.of(Role.CITIZEN));
        u1.setEnabled(true);
        u1 = userRepository.save(u1);
        user1Id = u1.getId();

        User u2 = new User();
        u2.setEmail("citizen2@test.com");
        u2.setPhone("5550000102");
        u2.setPasswordHash(passwordEncoder.encode("Password123!"));
        u2.setRoles(Set.of(Role.CITIZEN));
        u2.setEnabled(true);
        u2 = userRepository.save(u2);
        user2Id = u2.getId();

        deptId = seedDepartment("Zabıta");
        catId = seedCategory("Gürültü", deptId);

        token1 = loginAndGetToken("citizen1@test.com", "Password123!");
        token2 = loginAndGetToken("citizen2@test.com", "Password123!");
    }

    private String loginAndGetToken(String emailOrPhone, String pass) throws Exception {
        LoginRequest lr = new LoginRequest();
        lr.setEmailOrPhone(emailOrPhone);
        lr.setPassword(pass);

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(lr)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(loginJson).get("accessToken").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
        return token;
    }



    private Long seedDepartment(String name) {
        Department d = new Department();
        d.setName(name);
        d.setActive(true);
        d = departmentRepository.save(d);
        return d.getId();
    }

    private Long seedCategory(String name, Long deptId) {
        Department dept = departmentRepository.findById(deptId).orElseThrow();

        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setActive(true);
        c.setDefaultDepartment(dept);
        c = complaintCategoryRepository.save(c);
        return c.getId();
    }

    private Long seedComplaint(Long citizenId, Long categoryId, String title) {
        User user = userRepository.findById(citizenId).orElseThrow();
        ComplaintCategory cat = complaintCategoryRepository.findById(categoryId).orElseThrow();

        Complaint comp = new Complaint();
        comp.setCreatedBy(user);
        comp.setCategory(cat);
        comp.setDepartment(cat.getDefaultDepartment());

        comp.setTitle(title);
        comp.setDescription("desc-" + title);

        comp.setTrackingCode("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        comp.setStatus(ComplaintStatus.NEW);

        try { comp.setUpdatedAt(OffsetDateTime.now()); } catch (Exception ignored) {}
        try { comp.setCreatedAt(OffsetDateTime.now()); } catch (Exception ignored) {}

        comp = complaintRepository.save(comp);
        return comp.getId();
    }

    private Notification notif(Long userId, Long complaintId, String title, boolean isRead, OffsetDateTime createdAt) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setComplaintId(complaintId);
        n.setTitle(title);
        n.setBody("body-" + title);
        n.setLink("/complaints/" + (complaintId == null ? 0 : complaintId));
        n.setRead(isRead);
        n.setCreatedAt(createdAt);
        return notificationRepository.save(n);
    }


    @Test
    void list_shouldReturnOnlyMyNotifications_sortedByCreatedAtDesc_andPageableWorks() throws Exception {
        Long c1 = seedComplaint(user1Id, catId, "c1");
        Long c2 = seedComplaint(user1Id, catId, "c2");
        Long cOther = seedComplaint(user2Id, catId, "cOther");

        notif(user1Id, c1, "n1-old", false, OffsetDateTime.now().minusDays(2));
        notif(user1Id, c2, "n1-new", true,  OffsetDateTime.now().minusHours(1));
        notif(user2Id, cOther, "n2", false, OffsetDateTime.now().minusMinutes(5));

        mvc.perform(get("/api/citizen/notifications?page=0&size=1&sort=createdAt,desc")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("n1-new"))
                .andExpect(jsonPath("$.content[0].complaintId").value(c2))
                .andExpect(jsonPath("$.content[0].isRead").value(true));

        mvc.perform(get("/api/citizen/notifications?page=1&size=1&sort=createdAt,desc")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("n1-old"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    void unreadCount_shouldReturnCountOfUnreadOnly_forCurrentUser() throws Exception {
        Long c1 = seedComplaint(user1Id, catId, "u1-c1");
        Long c2 = seedComplaint(user1Id, catId, "u1-c2");
        Long c3 = seedComplaint(user1Id, catId, "u1-c3");
        Long cOther = seedComplaint(user2Id, catId, "u2-c1");

        notif(user1Id, c1, "u1-unread-1", false, OffsetDateTime.now().minusHours(3));
        notif(user1Id, c2, "u1-read",     true,  OffsetDateTime.now().minusHours(2));
        notif(user1Id, c3, "u1-unread-2", false, OffsetDateTime.now().minusHours(1));
        notif(user2Id, cOther, "u2-unread", false, OffsetDateTime.now().minusMinutes(5));

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void markRead_should204_andOnlyAffectMyNotification() throws Exception {
        Long cMine = seedComplaint(user1Id, catId, "mine-c");
        Long cOther = seedComplaint(user2Id, catId, "other-c");

        Notification mine = notif(user1Id, cMine, "mine", false, OffsetDateTime.now().minusHours(1));
        Notification other = notif(user2Id, cOther, "other", false, OffsetDateTime.now().minusMinutes(10));

        mvc.perform(patch("/api/citizen/notifications/" + mine.getId() + "/read")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());

        Notification mineAfter = notificationRepository.findById(mine.getId()).orElseThrow();
        assertTrue(mineAfter.isRead(), "my notification should be marked read");

        Notification otherAfter = notificationRepository.findById(other.getId()).orElseThrow();
        assertFalse(otherAfter.isRead(), "other user's notification must not be affected");

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void markAllRead_shouldReturnUpdatedCount_andThenUnreadCountZero() throws Exception {
        Long c1 = seedComplaint(user1Id, catId, "a-c");
        Long c2 = seedComplaint(user1Id, catId, "b-c");
        Long c3 = seedComplaint(user1Id, catId, "c-c");
        Long cOther = seedComplaint(user2Id, catId, "u2-c");

        notif(user1Id, c1, "a", false, OffsetDateTime.now().minusHours(3));
        notif(user1Id, c2, "b", false, OffsetDateTime.now().minusHours(2));
        notif(user1Id, c3, "c", true,  OffsetDateTime.now().minusHours(1));

        notif(user2Id, cOther, "u2", false, OffsetDateTime.now().minusMinutes(5));

        mvc.perform(patch("/api/citizen/notifications/read-all")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }
}
