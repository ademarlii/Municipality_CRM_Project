package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import com.ademarli.municipality_service.model.dto.complaint.CreateComplaintRequest;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintBusinessFlowIT extends BaseIntegrationTest {



    @Autowired
    UserRepository userRepository;
    @Autowired
    ComplaintRepository complaintRepository;


    @BeforeEach
    void clean() {
        complaintRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerLoginToken(String email, String phone, String pass) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPhone(phone);
        reg.setPassword(pass);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmailOrPhone(email);
        login.setPassword(pass);

        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("accessToken").asText();
    }

    @Test
    void createComplaint_shouldPersist_andTriggerSideEffect() throws Exception {
        String token = registerLoginToken("flow@test.com", "5550001111", "Password123!");


        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setTitle("Su kesintisi");
        req.setDescription("2 saattir su yok");
        req.setCategoryId(1L);

        mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        var all = complaintRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Su kesintisi");
    }
}
