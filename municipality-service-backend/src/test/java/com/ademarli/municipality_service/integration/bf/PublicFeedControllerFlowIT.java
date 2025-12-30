package com.ademarli.municipality_service.integration.bf;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicFeedControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    ComplaintRepository complaintRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    ComplaintCategoryRepository categoryRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void public_feed_returns_resolved_items() throws Exception {
        User citizen = new User();
        citizen.setEmail("pubcit@test.com");
        citizen.setPhone("5577000007");
        citizen.setPasswordHash(passwordEncoder.encode("Password123!"));
        citizen.setRoles(Set.of(Role.CITIZEN));
        userRepository.save(citizen);

        Department d = new Department();
        d.setName("PubD");
        d.setActive(true);
        departmentRepository.save(d);

        ComplaintCategory cat = new ComplaintCategory();
        cat.setName("PubCat");
        cat.setDefaultDepartment(d);
        cat.setActive(true);
        categoryRepository.save(cat);

        Complaint c = new Complaint();
        c.setTrackingCode("TRK-PUB-1");
        c.setTitle("pub");
        c.setCreatedBy(citizen);
        c.setDepartment(d);
        c.setCategory(cat);
        c.setStatus(ComplaintStatus.RESOLVED);
        c.setPublicAnswer("ans");
        c.setResolvedAt(OffsetDateTime.now());
        complaintRepository.save(c);

        mvc.perform(get("/api/public/feed?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].trackingCode").value("TRK****-1"));
    }
}
