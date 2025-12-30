package com.ademarli.municipality_service.integration.bf;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.Notification;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.NotificationRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CitizenNotificationControllerFlowIT extends BaseIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void notifications_list_and_mark_read_flow() throws Exception {
        User u = new User();
        u.setEmail("notify@test.com");
        u.setPhone("5566000006");
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(Role.CITIZEN));
        userRepository.save(u);

        Notification n1 = new Notification(null, u.getId(), null, "t1", "b1", null, false, OffsetDateTime.now());
        Notification n2 = new Notification(null, u.getId(), null, "t2", "b2", null, false, OffsetDateTime.now());
        notificationRepository.save(n1);
        notificationRepository.save(n2);

        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"emailOrPhone\":\"%s\",\"password\":\"Password123!\"}", u.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(login).get("accessToken").asText();

        mvc.perform(get("/api/citizen/notifications?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").exists());

        mvc.perform(get("/api/citizen/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        mvc.perform(patch("/api/citizen/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(patch("/api/citizen/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }
}
