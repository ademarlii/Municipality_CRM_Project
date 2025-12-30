package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
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

class PublicTrackingControllerIT extends BaseIntegrationTest {

    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ComplaintCategoryRepository complaintCategoryRepository;
    @Autowired ComplaintRepository complaintRepository;

    private String existingTrackingCode;
    private Long deptId;

    @BeforeEach
    void setup() {
        complaintRepository.deleteAllInBatch();
        complaintCategoryRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        deptId = seedDepartment("Zabıta");
        Long catId = seedCategory("Gürültü", deptId);
        Long citizenId = seedCitizen("citizen@test.com", "5550000101");

        existingTrackingCode = seedComplaintWithTracking(citizenId, catId, deptId, ComplaintStatus.IN_REVIEW);
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

    private String seedComplaintWithTracking(Long citizenId, Long catId, Long deptId, ComplaintStatus status) {
        User citizen = userRepository.findById(citizenId).orElseThrow();
        ComplaintCategory cat = complaintCategoryRepository.findById(catId).orElseThrow();
        Department dept = departmentRepository.findById(deptId).orElseThrow();

        String code = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Complaint c = new Complaint();
        c.setCreatedBy(citizen);
        c.setCategory(cat);
        c.setDepartment(dept);

        c.setTitle("Track test");
        c.setDescription("desc");
        c.setTrackingCode(code);
        c.setStatus(status);

        c.setCreatedAt(OffsetDateTime.now().minusDays(1));
        c.setUpdatedAt(OffsetDateTime.now());

        complaintRepository.save(c);
        return code;
    }


    @Test
    void track_existingTrackingCode_should200_andReturnStatusAndDepartmentName() throws Exception {
        mvc.perform(get("/api/public/complaints/track/" + existingTrackingCode)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value(existingTrackingCode))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.departmentName").value("Zabıta"));
    }

    @Test
    void track_unknownTrackingCode_should404() throws Exception {
        mvc.perform(get("/api/public/complaints/track/TRK-NOTEXIST")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
