package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.feedback.UpsertFeedbackRequest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CitizenFeedbackControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired FeedbackRepository feedbackRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired ComplaintCategoryRepository categoryRepository;
    @Autowired DepartmentRepository departmentRepository;

    private String citizenToken;
    private Long citizenId;

    private String agentToken;
    private Long agentId;

    @BeforeEach
    void setup() throws Exception {
        feedbackRepository.deleteAll();
        complaintRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();


        User citizen = new User();
        citizen.setEmail("citizen@test.com");
        citizen.setPhone("5550000001");
        citizen.setPasswordHash(passwordEncoder.encode("Password123!"));
        citizen.setRoles(Set.of(Role.CITIZEN));
        citizen.setEnabled(true);
        userRepository.save(citizen);
        citizenId = citizen.getId();

        User agent = new User();
        agent.setEmail("agent@test.com");
        agent.setPhone("5550000002");
        agent.setPasswordHash(passwordEncoder.encode("Password123!"));
        agent.setRoles(Set.of(Role.AGENT));
        agent.setEnabled(true);
        userRepository.save(agent);
        agentId = agent.getId();

        citizenToken = loginAndGetToken("citizen@test.com", "Password123!");
        agentToken = loginAndGetToken("agent@test.com", "Password123!");
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

    private Complaint complaint(User createdBy, ComplaintCategory category, Department department, ComplaintStatus status) {
        Complaint c = new Complaint();
        c.setCreatedBy(createdBy);
        c.setCategory(category);
        c.setDepartment(department);

        c.setTitle("Test Complaint");
        c.setDescription("desc");

        c.setTrackingCode("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c.setStatus(status);

        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        if (status == ComplaintStatus.RESOLVED) {
            c.setResolvedAt(OffsetDateTime.now());
            c.setPublicAnswer("Çözüldü");
        }
        if (status == ComplaintStatus.CLOSED) {
            c.setClosedAt(OffsetDateTime.now());
        }

        return complaintRepository.save(c);
    }

    private UpsertFeedbackRequest ratingReq(Integer rating) {
        UpsertFeedbackRequest r = new UpsertFeedbackRequest();
        r.setRating(rating);
        return r;
    }

    @Test
    void upsertRating_ok_firstTime_should200_andReturnStats_thenMyRatingAndStatsEndpointsWork() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(cmp.getId()))
                .andExpect(jsonPath("$.avgRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.myRating").value(5));


        mvc.perform(get("/api/citizen/feedback/" + cmp.getId() + "/rating/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));


        mvc.perform(get("/api/citizen/feedback/" + cmp.getId() + "/rating/stats")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(cmp.getId()))
                .andExpect(jsonPath("$.avgRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.myRating").value(5));
    }

    @Test
    void upsertRating_ok_updateExisting_shouldUpdateAvgAndMyRating() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);


        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(5))))
                .andExpect(status().isOk());

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.avgRating").value(3.0))
                .andExpect(jsonPath("$.myRating").value(3));
    }

    @Test
    void upsertRating_userNotCitizen_should403() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(5))))
                .andExpect(status().isForbidden());
    }

    @Test
    void upsertRating_ratingNull_should409_RATING_REQUIRED() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        UpsertFeedbackRequest req = new UpsertFeedbackRequest(); // rating null

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_REQUIRED"));
    }

    @Test
    void upsertRating_outOfRange_should409_RATING_MUST_BE_BETWEEN_1_AND_5() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_MUST_BE_BETWEEN_1_AND_5"));

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_MUST_BE_BETWEEN_1_AND_5"));
    }

    @Test
    void upsertRating_complaintNotFound_should404_COMPLAINT_NOT_FOUND() throws Exception {
        mvc.perform(put("/api/citizen/feedback/999999/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(5))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPLAINT_NOT_FOUND"));
    }

    @Test
    void upsertRating_complaintNotRateable_should409_ONLY_RESOLVED_OR_CLOSED_CAN_BE_RATED() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.NEW); // rateable değil

        mvc.perform(put("/api/citizen/feedback/" + cmp.getId() + "/rating")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ratingReq(5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONLY_RESOLVED_OR_CLOSED_CAN_BE_RATED"));
    }


    @Test
    void stats_noRatingsYet_should200_andReturnZeros_andMyRatingNull() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        mvc.perform(get("/api/citizen/feedback/" + cmp.getId() + "/rating/stats")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(cmp.getId()))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.myRating").doesNotExist());
    }

    @Test
    void myRating_noRatingYet_should200_andBodyNull() throws Exception {
        Department d = dept("Zabıta", true);
        ComplaintCategory category = cat("Gürültü", true, d);

        User citizen = userRepository.findById(citizenId).orElseThrow();
        Complaint cmp = complaint(citizen, category, d, ComplaintStatus.RESOLVED);

        String body = mvc.perform(get("/api/citizen/feedback/" + cmp.getId() + "/rating/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.isBlank() || body.equals("null"));
    }
}
