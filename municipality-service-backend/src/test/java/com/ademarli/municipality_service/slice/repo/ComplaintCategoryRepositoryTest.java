package com.ademarli.municipality_service.slice.repo;

import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplaintCategoryRepositoryTest extends BaseDataJpaTest {

    @Autowired ComplaintCategoryRepository categoryRepo;
    @Autowired DepartmentRepository departmentRepo;

    @BeforeEach
    void cleanup() {
        categoryRepo.deleteAll();
        departmentRepo.deleteAll();
    }


    @Test
    void findByNameIgnoreCase_shouldWork() {
        Department d = new Department();
        d.setName("gfnfgndfnsdgd");
        d.setActive(true);
        departmentRepo.save(d);

        ComplaintCategory c = new ComplaintCategory();
        c.setName("adem");
        c.setActive(true);
        c.setDefaultDepartment(d);
        categoryRepo.save(c);

        var opt = categoryRepo.findByNameIgnoreCase("ADem");
        assertThat(opt).isPresent();
        assertThat(opt.get().getName()).isEqualTo("adem");
        assertThat(opt.get().getDefaultDepartment().getId()).isEqualTo(d.getId());
    }

    @Test
    void findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc_shouldFilterAndSort() {
        Department d = new Department();
        d.setName("Temizlik");
        d.setActive(true);
        departmentRepo.save(d);

        ComplaintCategory a = new ComplaintCategory();
        a.setName("A Kategori");
        a.setActive(true);
        a.setDefaultDepartment(d);

        ComplaintCategory b = new ComplaintCategory();
        b.setName("B Kategori");
        b.setActive(true);
        b.setDefaultDepartment(d);

        ComplaintCategory passive = new ComplaintCategory();
        passive.setName("Pasif");
        passive.setActive(false);
        passive.setDefaultDepartment(d);

        categoryRepo.saveAll(List.of(b, passive, a));

        List<ComplaintCategory> list =
                categoryRepo.findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(d.getId());

        assertThat(list).extracting(ComplaintCategory::getName)
                .containsExactly("A Kategori", "B Kategori");
    }
}
