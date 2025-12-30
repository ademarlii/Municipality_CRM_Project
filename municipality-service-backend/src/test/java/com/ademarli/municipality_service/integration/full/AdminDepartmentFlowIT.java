package com.ademarli.municipality_service.integration.full;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class AdminDepartmentFlowIT extends BaseIntegrationTest {

    @Test
    void scenario2_adminCreate_list_update_delete_department() throws Exception {

        String created = mvc.perform(post("/api/admin/departments")
                        .with(user("999").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Temizlik","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Temizlik"))
                .andReturn().getResponse().getContentAsString();

        long deptId = om.readTree(created).get("id").asLong();

        mvc.perform(get("/api/admin/departments?page=0&size=10")
                        .with(user("999").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mvc.perform(put("/api/admin/departments/" + deptId)
                        .with(user("999").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Temizlik-2","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deptId))
                .andExpect(jsonPath("$.name").value("Temizlik-2"));

        mvc.perform(delete("/api/admin/departments/" + deptId)
                        .with(user("999").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
