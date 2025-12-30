package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.*;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicFeedControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired FeedbackRepository feedbackRepository;

    private Long deptId;
    private Long catId;

    private Long citizen1Id;
    private Long citizen2Id;

    private Long complaintNewerId;
    private Long complaintOlderId;
    private Long complaintNotResolvedId;

    @BeforeEach
    void setup() {
        feedbackRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        complaintCategoryRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        deptId = seedDepartment("Zabıta");
        catId = seedCategory("Gürültü", deptId);

        citizen1Id = seedCitizen("citizen1@test.com", "5550000101");
        citizen2Id = seedCitizen("citizen2@test.com", "5550000102");

        complaintOlderId = seedComplaintResolved(
                citizen1Id, catId,
                "Eski çözüm", "Eski çözüm cevabı",
                OffsetDateTime.now().minusDays(2),
                "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        complaintNewerId = seedComplaintResolved(
                citizen1Id, catId,
                "Yeni çözüm", "Yeni çözüm cevabı",
                OffsetDateTime.now().minusHours(1),
                "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        complaintNotResolvedId = seedComplaintNotResolved(
                citizen1Id, catId,
                "Henüz çözülmedi",
                ComplaintStatus.IN_REVIEW,
                "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        seedFeedback(complaintNewerId, citizen1Id, 5);
        seedFeedback(complaintNewerId, citizen2Id, 3);

        seedFeedback(complaintOlderId, citizen2Id, 2);
    }

    private Long seedCitizen(String email, String phone) {
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(Role.CITIZEN));
        u.setEnabled(true);
        u = userRepository.save(u);
        return u.getId();
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

    private Long seedComplaintResolved(
            Long citizenId,
            Long categoryId,
            String title,
            String publicAnswer,
            OffsetDateTime answeredAt,
            String trackingCode
    ) {
        User user = userRepository.findById(citizenId).orElseThrow();
        ComplaintCategory cat = complaintCategoryRepository.findById(categoryId).orElseThrow();

        Complaint comp = new Complaint();
        comp.setCreatedBy(user);
        comp.setCategory(cat);
        comp.setDepartment(cat.getDefaultDepartment());

        comp.setTitle(title);
        comp.setDescription("desc-" + title);

        comp.setTrackingCode(trackingCode);
        comp.setStatus(ComplaintStatus.RESOLVED);

        comp.setUpdatedAt(OffsetDateTime.now());
        comp.setCreatedAt(OffsetDateTime.now().minusDays(3));

        comp.setPublicAnswer(publicAnswer);
        comp.setResolvedAt(answeredAt);

        comp = complaintRepository.save(comp);
        return comp.getId();
    }

    private Long seedComplaintNotResolved(
            Long citizenId,
            Long categoryId,
            String title,
            ComplaintStatus status,
            String trackingCode
    ) {
        User user = userRepository.findById(citizenId).orElseThrow();
        ComplaintCategory cat = complaintCategoryRepository.findById(categoryId).orElseThrow();

        Complaint comp = new Complaint();
        comp.setCreatedBy(user);
        comp.setCategory(cat);
        comp.setDepartment(cat.getDefaultDepartment());

        comp.setTitle(title);
        comp.setDescription("desc-" + title);

        comp.setTrackingCode(trackingCode);
        comp.setStatus(status);

        comp.setUpdatedAt(OffsetDateTime.now());
        comp.setCreatedAt(OffsetDateTime.now().minusDays(1));

        comp = complaintRepository.save(comp);
        return comp.getId();
    }

    private void seedFeedback(Long complaintId, Long citizenId, int rating) {
        Complaint c = complaintRepository.findById(complaintId).orElseThrow();
        User citizen = userRepository.findById(citizenId).orElseThrow();

        Feedback fb = new Feedback();
        fb.setComplaint(c);
        fb.setCitizen(citizen);
        fb.setRating(rating);

        feedbackRepository.save(fb);
    }

    private String expectedMasked(String trackingCode) {

        String start = trackingCode.substring(0, 3);
        String end = trackingCode.substring(trackingCode.length() - 2);
        return start + "****" + end;
    }


    @Test
    void feed_shouldReturnOnlyResolved_sortedByAnsweredAtDesc_withRatingsAndMaskedTrackingCode() throws Exception {
        String newerCode = complaintRepository.findById(complaintNewerId).orElseThrow().getTrackingCode();
        String olderCode = complaintRepository.findById(complaintOlderId).orElseThrow().getTrackingCode();

        mvc.perform(get("/api/public/feed?page=0&size=10&sort=answeredAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.content.length()").value(2))


                .andExpect(jsonPath("$.content[0].id").value(complaintNewerId))
                .andExpect(jsonPath("$.content[0].title").value("Yeni çözüm"))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[0].publicAnswer").value("Yeni çözüm cevabı"))
                .andExpect(jsonPath("$.content[0].departmentName").value("Zabıta"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Gürültü"))
                .andExpect(jsonPath("$.content[0].trackingCode").value(expectedMasked(newerCode)))
                .andExpect(jsonPath("$.content[0].avgRating").value(4.0))
                .andExpect(jsonPath("$.content[0].ratingCount").value(2))

                .andExpect(jsonPath("$.content[1].id").value(complaintOlderId))
                .andExpect(jsonPath("$.content[1].title").value("Eski çözüm"))
                .andExpect(jsonPath("$.content[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[1].publicAnswer").value("Eski çözüm cevabı"))
                .andExpect(jsonPath("$.content[1].trackingCode").value(expectedMasked(olderCode)))
                .andExpect(jsonPath("$.content[1].avgRating").value(2.0))
                .andExpect(jsonPath("$.content[1].ratingCount").value(1));
    }

    @Test
    void feed_shouldNotContainNonResolvedComplaints() throws Exception {
        mvc.perform(get("/api/public/feed?page=0&size=50&sort=answeredAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)]".formatted(complaintNotResolvedId)).doesNotExist());
    }
}
