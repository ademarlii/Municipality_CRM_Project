package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.CategoryResponse;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.service.AdminCategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock ComplaintCategoryRepository categoryRepository;
    @Mock DepartmentRepository departmentRepository;

    @InjectMocks AdminCategoryService service;

    private Department dept(Long id, boolean active, String name) {
        Department d = new Department();
        d.setId(id);
        d.setActive(active);
        d.setName(name);
        return d;
    }

    private ComplaintCategory category(Long id, String name, boolean active, Department d) {
        ComplaintCategory c = new ComplaintCategory();
        c.setId(id);
        c.setName(name);
        c.setActive(active);
        c.setDefaultDepartment(d);
        return c;
    }

    private CategoryUpsertRequest req(String name, Long deptId, Boolean active) {
        CategoryUpsertRequest r = new CategoryUpsertRequest();
        r.setName(name);
        r.setDefaultDepartmentId(deptId);
        r.setActive(active);
        return r;
    }

    // oluşturma

    @Test
    void create_ok_kaydiOlusturupResponseDonmeli() {
        var request = req("  Çöp Toplama  ", 2L, true);
        Department d = dept(2L, true, "Temizlik İşleri");

        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama")).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(d));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse out = service.create(request);

        verify(categoryRepository).save(argThat(saved ->
                saved != null
                        && "Çöp Toplama".equals(saved.getName())
                        && saved.getDefaultDepartment() == d
                        && saved.isActive()
        ));

        assertNotNull(out);
        assertEquals("Çöp Toplama", out.getName());
        assertTrue(out.isActive());
        assertEquals(2L, out.getDefaultDepartmentId());
        assertEquals("Temizlik İşleri", out.getDefaultDepartmentName());
    }

    @Test
    void create_isimZatenVarsa_businessHatasi() {
        var request = req("Gürültü", 2L, true);

        when(categoryRepository.findByNameIgnoreCase("Gürültü"))
                .thenReturn(Optional.of(category(9L, "Gürültü", true, dept(2L, true, "Zabıta"))));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", ex.getMessage());

        verifyNoInteractions(departmentRepository);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_departmanBulunamazsa_notFound() {
        var request = req("Çöp Toplama", 999L, true);

        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama")).thenReturn(Optional.empty());
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.create(request));
        assertEquals("DEFAULT_DEPARTMENT_NOT_FOUND", ex.getMessage());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_departmanPasifse_businessHatasi() {
        var request = req("Çöp Toplama", 2L, true);

        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama")).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(dept(2L, false, "Temizlik İşleri")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_activeNull_ise_defaultDegerKorunmali() {
        var request = req("Çöp Toplama", 2L, null);
        Department d = dept(2L, true, "Temizlik İşleri");

        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama")).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(d));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request);

        verify(categoryRepository).save(argThat(saved ->
                saved != null
                        && "Çöp Toplama".equals(saved.getName())
                        && saved.getDefaultDepartment() == d
                        // entity default'u true ise bu geçer
                        && saved.isActive()
        ));
    }

    // listeleme

    @Test
    void list_ok_sayfayiResponseaMaplemeli() {
        Department d1 = dept(2L, true, "Temizlik İşleri");
        ComplaintCategory c1 = category(1L, "Çöp Toplama", true, d1);

        Page<ComplaintCategory> page = new PageImpl<>(
                List.of(c1),
                PageRequest.of(0, 10, Sort.by("id").ascending()),
                1
        );

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<CategoryResponse> out = service.list(PageRequest.of(0, 10));

        assertEquals(1, out.getTotalElements());
        CategoryResponse r = out.getContent().getFirst();
        assertEquals(1L, r.getId());
        assertEquals("Çöp Toplama", r.getName());
        assertTrue(r.isActive());
        assertEquals(2L, r.getDefaultDepartmentId());
        assertEquals("Temizlik İşleri", r.getDefaultDepartmentName());

        verify(categoryRepository).findAll(any(Pageable.class));
    }

    // güncelleme

    @Test
    void update_ok_isimDegisirse_uniqueKontroluYapipKaydetmeli() {
        Department oldDept = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", true, oldDept);

        Department newDept = dept(2L, true, "Temizlik İşleri");
        var request = req("  Çöp Toplama  ", 2L, false);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama")).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse out = service.update(10L, request);

        verify(categoryRepository).save(argThat(saved ->
                saved != null
                        && "Çöp Toplama".equals(saved.getName())
                        && saved.getDefaultDepartment() == newDept
                        && !saved.isActive()
        ));

        assertEquals(10L, out.getId());
        assertEquals("Çöp Toplama", out.getName());
        assertFalse(out.isActive());
        assertEquals(2L, out.getDefaultDepartmentId());
        assertEquals("Temizlik İşleri", out.getDefaultDepartmentName());

        verify(categoryRepository).findByNameIgnoreCase("Çöp Toplama");
    }

    @Test
    void update_ok_isimAyniIgnoreCaseIse_uniqueKontroluYapmamalı() {
        Department oldDept = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "GÜRÜLTÜ", true, oldDept);

        Department newDept = dept(2L, true, "Temizlik İşleri");
        var request = req("gürültü", 2L, true);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(10L, request);

        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository).save(argThat(saved ->
                saved != null
                        && "GÜRÜLTÜ".equals(saved.getName()) // değişmemeli
                        && saved.getDefaultDepartment() == newDept
                        && saved.isActive()
        ));
    }

    @Test
    void update_categoryBulunamazsa_notFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.update(99L, req("X", 1L, true))
        );
        assertEquals("CATEGORY_NOT_FOUND", ex.getMessage());

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void update_yeniIsimZatenVarsa_businessHatasi() {
        Department d = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", true, d);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Çöp Toplama"))
                .thenReturn(Optional.of(category(11L, "Çöp Toplama", true, d)));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.update(10L, req("Çöp Toplama", 1L, true))
        );
        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", ex.getMessage());

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void update_departmanBulunamazsa_notFound() {
        Department d = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", true, d);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.update(10L, req("Gürültü", 999L, true))
        );
        assertEquals("DEFAULT_DEPARTMENT_NOT_FOUND", ex.getMessage());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_departmanPasifse_businessHatasi() {
        Department d = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", true, d);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(dept(2L, false, "Temizlik İşleri")));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.update(10L, req("Gürültü", 2L, true))
        );
        assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_activeNull_ise_mevcutActiveKorunmali() {
        Department oldDept = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", false, oldDept); // aktif false

        Department newDept = dept(2L, true, "Temizlik İşleri");
        var request = req("Gürültü", 2L, null); // active null

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(10L, request);

        verify(categoryRepository).save(argThat(saved ->
                saved != null
                        && saved.getDefaultDepartment() == newDept
                        && !saved.isActive() // null geldiği için değişmemeli
        ));
    }

    // silme (soft delete)

    @Test
    void delete_ok_activeFalseYapmali() {
        Department d = dept(1L, true, "Zabıta");
        ComplaintCategory existing = category(10L, "Gürültü", true, d);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(ComplaintCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(10L);

        verify(categoryRepository).save(argThat(saved ->
                saved != null && !saved.isActive()
        ));
    }

    @Test
    void delete_categoryBulunamazsa_notFound() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.delete(10L));
        assertEquals("CATEGORY_NOT_FOUND", ex.getMessage());

        verify(categoryRepository, never()).save(any());
    }
}
