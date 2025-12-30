package com.ademarli.municipality_service.integration.flow;

import com.ademarli.municipality_service.integration.BaseIntegrationTest;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicCatalogControllerIT extends BaseIntegrationTest {

    @Autowired DepartmentRepository departmentRepository;
    @Autowired ComplaintCategoryRepository categoryRepository;

    private Long deptAId;
    private Long deptBId;

    @BeforeEach
    void setup() {
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();

        Department d1 = new Department();
        d1.setName("Zabıta");
        d1.setActive(true);
        departmentRepository.save(d1);
        deptAId = d1.getId();

        Department d2 = new Department();
        d2.setName("Temizlik");
        d2.setActive(true);
        departmentRepository.save(d2);
        deptBId = d2.getId();

        Department d3 = new Department();
        d3.setName("Park Bahçe");
        d3.setActive(false);
        departmentRepository.save(d3);

        saveCat("Gürültü", true, d1);
        saveCat("Asayiş", true, d1);
        saveCat("Pasif Kategori", false, d1);

        saveCat("Çöp", true, d2);
        saveCat("Süpürme", true, d2);
    }

    private void saveCat(String name, boolean active, Department dept) {
        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setActive(active);
        c.setDefaultDepartment(dept);
        categoryRepository.save(c);
    }

    @Test
    void departments_shouldReturnOnlyActive_sortedByNameAsc() throws Exception {

        mvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Temizlik"))
                .andExpect(jsonPath("$[1].name").value("Zabıta"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].id").exists());
    }

    @Test
    void categories_shouldReturnOnlyActiveByDepartment_sortedByNameAsc() throws Exception {
        mvc.perform(get("/api/public/departments/" + deptAId + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Asayiş"))
                .andExpect(jsonPath("$[1].name").value("Gürültü"))
                .andExpect(jsonPath("$[0].id").exists());


        mvc.perform(get("/api/public/departments/" + deptBId + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name").isArray())
                .andExpect(jsonPath("$[?(@.name=='Çöp')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Süpürme')]").exists());
    }

    @Test
    void categories_shouldNotLeakOtherDepartmentsCategories() throws Exception {

        mvc.perform(get("/api/public/departments/" + deptAId + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Çöp')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name=='Süpürme')]").doesNotExist());
    }
}
