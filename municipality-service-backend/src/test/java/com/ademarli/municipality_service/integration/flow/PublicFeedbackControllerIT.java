package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.Feedback;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.FeedbackRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicFeedbackControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired FeedbackRepository feedbackRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired DepartmentRepository departmentRepository;

    private Long deptId;
    private Long catId;

    private Long complaintId;
    private Long citizen1Id;
    private Long citizen2Id;

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

        complaintId = seedComplaint(citizen1Id, catId, "Çöp sorunu", ComplaintStatus.RESOLVED);
    }


    private Long seedCitizen(String email, String phone) {
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        try { u.setPasswordHash(passwordEncoder.encode("Password123!")); } catch (Exception ignored) {}
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

    private Long seedComplaint(Long citizenId, Long categoryId, String title, ComplaintStatus status) {
        User user = userRepository.findById(citizenId).orElseThrow();
        ComplaintCategory cat = complaintCategoryRepository.findById(categoryId).orElseThrow();

        Complaint comp = new Complaint();
        comp.setCreatedBy(user);
        comp.setCategory(cat);
        comp.setDepartment(cat.getDefaultDepartment());

        comp.setTitle(title);
        comp.setDescription("desc-" + title);

        comp.setTrackingCode("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        comp.setStatus(status);

        try { comp.setUpdatedAt(OffsetDateTime.now()); } catch (Exception ignored) {}
        try { comp.setCreatedAt(OffsetDateTime.now()); } catch (Exception ignored) {}

        if (status == ComplaintStatus.RESOLVED) {
            try { comp.setResolvedAt(OffsetDateTime.now()); } catch (Exception ignored) {}
            try { comp.setPublicAnswer("Çözüldü"); } catch (Exception ignored) {}
        }
        if (status == ComplaintStatus.CLOSED) {
            try { comp.setClosedAt(OffsetDateTime.now()); } catch (Exception ignored) {}
        }

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

        try { fb.setCreatedAt(OffsetDateTime.now()); } catch (Exception ignored) {}

        feedbackRepository.save(fb);
    }

    @Test
    void stats_shouldReturnAvgAndCount_andMyRatingNull_forPublic() throws Exception {
        // citizen1: 5, citizen2: 3 => avg=4.0 count=2
        seedFeedback(complaintId, citizen1Id, 5);
        seedFeedback(complaintId, citizen2Id, 3);

        mvc.perform(get("/api/public/complaints/" + complaintId + "/rating/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaintId))
                .andExpect(jsonPath("$.ratingCount").value(2))
                .andExpect(jsonPath("$.avgRating").value(4.0))
                .andExpect(jsonPath("$.myRating").doesNotExist()); // public endpoint -> null/absent
    }

    @Test
    void stats_shouldReturnZeros_whenNoRatings() throws Exception {
        mvc.perform(get("/api/public/complaints/" + complaintId + "/rating/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaintId))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.myRating").doesNotExist());
    }
}
