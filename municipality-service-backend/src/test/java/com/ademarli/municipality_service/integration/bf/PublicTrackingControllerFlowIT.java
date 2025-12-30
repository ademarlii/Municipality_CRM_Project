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

class PublicTrackingControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    ComplaintRepository complaintRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    ComplaintCategoryRepository categoryRepository;

    @BeforeEach
    void clearData() {
        complaintRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void track_by_code_returns_ok_or_notfound() throws Exception {
        Department department=new Department();
        department.setName("Dept1");
        department.setActive(true);
        Department savedD=departmentRepository.save(department);



        ComplaintCategory category=new ComplaintCategory();
        category.setName("Category1");
        category.setDefaultDepartment(savedD);
        category.setActive(true);
        ComplaintCategory savedC=categoryRepository.save(category);

        User citizen = new User();
        citizen.setEmail("trackcit@test.com");
        citizen.setPhone("5588000008");
        citizen.setPasswordHash(passwordEncoder.encode("Password123!"));
        citizen.setRoles(Set.of(Role.CITIZEN));
        userRepository.save(citizen);

        Complaint c = new Complaint();
        c.setTrackingCode("TRK-TRACK-1");
        c.setTitle("track");
        c.setCreatedBy(citizen);
        c.setStatus(ComplaintStatus.NEW);
        c.setCreatedAt(OffsetDateTime.now());
        c.setCategory(savedC);
        c.setDepartment(savedD);
        complaintRepository.save(c);

        mvc.perform(get("/api/public/complaints/track/TRK-TRACK-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value("TRK-TRACK-1"));

        mvc.perform(get("/api/public/complaints/track/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

}
