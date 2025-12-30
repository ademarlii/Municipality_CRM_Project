package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.DepartmentResponse;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.service.AdminDepartmentService;
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
class AdminDepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;

    @InjectMocks AdminDepartmentService service;

    private DepartmentUpsertRequest req(String name, Boolean active) {
        DepartmentUpsertRequest r = new DepartmentUpsertRequest();
        r.setName(name);
        r.setActive(active);
        return r;
    }

    private Department dept(Long id, String name, boolean active) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setActive(active);
        return d;
    }

    // oluşturma

    @Test
    void create_ok_isimTrimlenipKaydedilmeli_veResponseDonmeli() {
        DepartmentUpsertRequest request = req("  Zabıta  ", true);

        when(departmentRepository.findByNameIgnoreCase("Zabıta")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(5L);
            return d;
        });

        DepartmentResponse out = service.create(request);

        assertNotNull(out);
        assertEquals(5L, out.getId());
        assertEquals("Zabıta", out.getName());
        assertTrue(out.isActive());

        verify(departmentRepository).findByNameIgnoreCase("Zabıta");
        verify(departmentRepository).save(argThat(d ->
                "Zabıta".equals(d.getName()) && d.isActive()
        ));
    }

    @Test
    void create_ayniIsimVarsa_businessHatasi_veKaydetmemeli() {
        DepartmentUpsertRequest request = req("Zabıta", true);

        when(departmentRepository.findByNameIgnoreCase("Zabıta"))
                .thenReturn(Optional.of(dept(1L, "Zabıta", true)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("DEPARTMENT_NAME_ALREADY_EXISTS", ex.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void create_activeNull_ise_servisAktifligiZorlamamali() {
        DepartmentUpsertRequest request = req("  Temizlik  ", null);

        when(departmentRepository.findByNameIgnoreCase("Temizlik")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(9L);
            return d;
        });

        DepartmentResponse out = service.create(request);

        assertNotNull(out);
        assertEquals(9L, out.getId());
        assertEquals("Temizlik", out.getName());

        // active default'u entity'de neyse o kalır, biz sadece name'in doğru kaydedildiğini kontrol ediyoruz
        verify(departmentRepository).save(argThat(d ->
                "Temizlik".equals(d.getName())
        ));
    }

    // listeleme

    @Test
    void list_ok_sayfayiResponseaMaplemeli() {
        Department d1 = dept(1L, "Zabıta", true);
        Department d2 = dept(2L, "Temizlik", false);

        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        when(departmentRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(d1, d2), pageable, 2));

        Page<DepartmentResponse> out = service.list(pageable);

        assertEquals(2, out.getTotalElements());
        assertEquals(2, out.getContent().size());

        DepartmentResponse r1 = out.getContent().get(0);
        assertEquals(1L, r1.getId());
        assertEquals("Zabıta", r1.getName());
        assertTrue(r1.isActive());

        DepartmentResponse r2 = out.getContent().get(1);
        assertEquals(2L, r2.getId());
        assertEquals("Temizlik", r2.getName());
        assertFalse(r2.isActive());

        verify(departmentRepository).findAll(pageable);
    }

    // güncelleme

    @Test
    void update_departmanYoksa_notFound() {
        when(departmentRepository.findById(5L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.update(5L, req("X", true))
        );
        assertEquals("DEPARTMENT_NOT_FOUND", ex.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_isimDegisirse_duplicateKontroluYapip_guncelleyipKaydetmeli() {
        Department existing = dept(5L, "Zabıta", true);

        when(departmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByNameIgnoreCase("Yeni Ad")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse out = service.update(5L, req("  Yeni Ad  ", false));

        assertNotNull(out);
        assertEquals(5L, out.getId());
        assertEquals("Yeni Ad", out.getName());
        assertFalse(out.isActive());

        verify(departmentRepository).findByNameIgnoreCase("Yeni Ad");
        verify(departmentRepository).save(argThat(d ->
                d.getId().equals(5L) &&
                        "Yeni Ad".equals(d.getName()) &&
                        !d.isActive()
        ));
    }

    @Test
    void update_isimDegisirse_amaDuplicateVarsa_businessHatasi_veKaydetmemeli() {
        Department existing = dept(5L, "Zabıta", true);

        when(departmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByNameIgnoreCase("Yeni Ad"))
                .thenReturn(Optional.of(dept(99L, "Yeni Ad", true)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(5L, req("Yeni Ad", true))
        );
        assertEquals("DEPARTMENT_NAME_ALREADY_EXISTS", ex.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_isimAyniIgnoreCaseIse_duplicateSorgusuYapmamalı() {
        Department existing = dept(5L, "Zabıta", true);

        when(departmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse out = service.update(5L, req("  zabıta  ", false));

        assertEquals("Zabıta", out.getName());
        assertFalse(out.isActive());

        verify(departmentRepository, never()).findByNameIgnoreCase(anyString());
        verify(departmentRepository).save(argThat(d ->
                d.getId().equals(5L) &&
                        "Zabıta".equals(d.getName()) &&
                        !d.isActive()
        ));
    }

    @Test
    void update_activeNull_ise_activeDegismemeli() {
        Department existing = dept(5L, "Zabıta", true);

        when(departmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse out = service.update(5L, req("  Zabıta  ", null));

        assertEquals("Zabıta", out.getName());
        assertTrue(out.isActive(), "active null ise entity active değişmemeli");

        verify(departmentRepository, never()).findByNameIgnoreCase(anyString());
        verify(departmentRepository).save(argThat(Department::isActive));
    }

    // silme (soft delete)

    @Test
    void delete_departmanYoksa_notFound() {
        when(departmentRepository.findById(7L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.delete(7L));
        assertEquals("DEPARTMENT_NOT_FOUND", ex.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void delete_ok_activeFalseYapipKaydetmeli() {
        Department existing = dept(7L, "Temizlik", true);

        when(departmentRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(7L);

        verify(departmentRepository).save(argThat(d ->
                d.getId().equals(7L) && !d.isActive()
        ));
    }
}
