package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.model.dto.publiccatalog.PublicCategoryItem;
import com.ademarli.municipality_service.model.dto.publiccatalog.PublicDepartmentItem;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.service.PublicCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock ComplaintCategoryRepository categoryRepository;

    @InjectMocks PublicCatalogService service;

    private Department dept(Long id, String name, boolean active) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setActive(active);
        return d;
    }

    private ComplaintCategory cat(Long id, String name, boolean active) {
        ComplaintCategory c = new ComplaintCategory();
        c.setId(id);
        c.setName(name);
        c.setActive(active);
        return c;
    }

    // departmanlar

    @Test
    void activeDepartments_shouldCallRepo_andMapToDto() {
        when(departmentRepository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(
                        dept(1L, "Temizlik", true),
                        dept(2L, "Zabıta", true)
                ));

        List<PublicDepartmentItem> out = service.activeDepartments();

        assertNotNull(out);
        assertEquals(2, out.size());

        assertEquals(1L, out.get(0).getId());
        assertEquals("Temizlik", out.get(0).getName());

        assertEquals(2L, out.get(1).getId());
        assertEquals("Zabıta", out.get(1).getName());

        verify(departmentRepository, times(1)).findByActiveTrueOrderByNameAsc();
        verify(categoryRepository, never()).findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(anyLong());
    }

    @Test
    void activeDepartments_whenEmpty_shouldReturnEmptyList() {
        when(departmentRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        List<PublicDepartmentItem> out = service.activeDepartments();

        assertNotNull(out);
        assertTrue(out.isEmpty());

        verify(departmentRepository, times(1)).findByActiveTrueOrderByNameAsc();
        verify(categoryRepository, never()).findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(anyLong());
    }

    // kategoriler

    @Test
    void activeCategoriesByDepartment_shouldCallRepoWithDeptId_andMapToDto() {
        Long deptId = 5L;

        when(categoryRepository.findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(deptId))
                .thenReturn(List.of(
                        cat(10L, "Gürültü", true),
                        cat(11L, "Çöp Toplama", true)
                ));

        List<PublicCategoryItem> out = service.activeCategoriesByDepartment(deptId);

        assertNotNull(out);
        assertEquals(2, out.size());

        assertEquals(10L, out.get(0).getId());
        assertEquals("Gürültü", out.get(0).getName());

        assertEquals(11L, out.get(1).getId());
        assertEquals("Çöp Toplama", out.get(1).getName());

        verify(categoryRepository, times(1))
                .findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(deptId);
        verify(departmentRepository, never()).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void activeCategoriesByDepartment_whenEmpty_shouldReturnEmptyList() {
        Long deptId = 5L;

        when(categoryRepository.findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(deptId))
                .thenReturn(List.of());

        List<PublicCategoryItem> out = service.activeCategoriesByDepartment(deptId);

        assertNotNull(out);
        assertTrue(out.isEmpty());

        verify(categoryRepository, times(1))
                .findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(deptId);
        verify(departmentRepository, never()).findByActiveTrueOrderByNameAsc();
    }
}
