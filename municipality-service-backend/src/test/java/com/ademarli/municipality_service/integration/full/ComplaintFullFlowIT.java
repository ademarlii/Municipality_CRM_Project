package com.ademarli.municipality_service.integration.full;


import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class ComplaintFullFlowIT extends BaseIntegrationTest {



    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    ComplaintCategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void seedCatalog() {
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();

        Department dep = new Department();
        dep.setName("Temizlik İşleri");
        dep.setActive(true);
        dep = departmentRepository.save(dep);

        ComplaintCategory cat = new ComplaintCategory();
        cat.setName("Çöp Toplama");
        cat.setActive(true);

        cat.setDefaultDepartment(dep);


        cat = categoryRepository.save(cat);

        categoryId = cat.getId();
    }

    private String registerAndLoginGetToken(String email, String phone, String pass) throws Exception {

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","phone":"%s","password":"%s"}
                                """.formatted(email, phone, pass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        String loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrPhone":"%s","password":"%s"}
                                """.formatted(email, pass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = om.readTree(loginJson);
        return node.get("accessToken").asText();
    }

    @Test
    void fullCitizenFlow_createAndListMyComplaints() throws Exception {
        String token = registerAndLoginGetToken(
                "citizen1@test.com",
                "5551112233",
                "Password123!"
        );

        String createdJson = mvc.perform(post("/api/citizen/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Çöp toplanmıyor",
                                  "description":"Mahallede 3 gündür çöp alınmıyor",
                                  "categoryId": %d,
                                  "lat": 38.35,
                                  "lon": 38.31
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Çöp toplanmıyor"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andReturn().getResponse().getContentAsString();
        Long complaintId = om.readTree(createdJson).get("id").asLong();
        mvc.perform(get("/api/citizen/complaints/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(complaintId))
                .andExpect(jsonPath("$[0].title").value("Çöp toplanmıyor"));
    }
}
