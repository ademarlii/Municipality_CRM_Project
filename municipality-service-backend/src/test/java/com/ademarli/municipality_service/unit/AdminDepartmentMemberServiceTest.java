package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentMemberResponse;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.DepartmentMember;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.DepartmentMemberRole;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.service.AdminDepartmentMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentMemberServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock UserRepository userRepository;
    @Mock DepartmentMemberRepository memberRepository;

    @InjectMocks AdminDepartmentMemberService service;

    private Department dept(Long id, boolean active) {
        Department d = new Department();
        d.setId(id);
        d.setActive(active);
        d.setName("Dept-" + id);
        return d;
    }

    private User user(Long id, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setEmail("u" + id + "@test.com");
        u.setPhone("555" + id);

        if (roles == null) {
            u.setRoles(null);
        } else {
            u.setRoles(new HashSet<>(Arrays.asList(roles)));
        }
        return u;
    }

    private DepartmentMember member(Department d, User u, DepartmentMemberRole role, boolean active) {
        DepartmentMember m = new DepartmentMember();
        m.setDepartment(d);
        m.setUser(u);
        m.setMemberRole(role);
        m.setActive(active);
        return m;
    }

    private AddDepartmentMemberRequest req(Long userId, String memberRole) {
        AddDepartmentMemberRequest r = new AddDepartmentMemberRequest();
        r.setUserId(userId);
        r.setMemberRole(memberRole);
        return r;
    }

    // ekleme

    @Test
    void addMember_departmanYoksa_notFound() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addMember(10L, req(1L, "MEMBER"))
        );
        assertEquals("DEPARTMENT_NOT_FOUND", ex.getMessage());

        verify(departmentRepository).findById(10L);
        verify(userRepository, never()).findById(anyLong());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMember_departmanPasifse_businessHatasi() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept(10L, false)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMember(10L, req(1L, "MEMBER"))
        );
        assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());

        verify(departmentRepository).findById(10L);
        verify(userRepository, never()).findById(anyLong());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMember_kullaniciYoksa_notFound() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept(10L, true)));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addMember(10L, req(1L, "MEMBER"))
        );
        assertEquals("USER_NOT_FOUND", ex.getMessage());

        verify(departmentRepository).findById(10L);
        verify(userRepository).findById(1L);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMember_agentDegilse_businessHatasi_rolesNull_veCitizen() {
        Department d = dept(10L, true);

        User rolesNull = user(1L);
        rolesNull.setRoles(null);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(rolesNull));

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> service.addMember(10L, req(1L, "MEMBER"))
        );
        assertEquals("ONLY_AGENT_CAN_BE_DEPARTMENT_MEMBER", ex1.getMessage());
        verify(memberRepository, never()).save(any());

        User citizen = user(2L, Role.CITIZEN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(citizen));

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> service.addMember(10L, req(2L, "MEMBER"))
        );
        assertEquals("ONLY_AGENT_CAN_BE_DEPARTMENT_MEMBER", ex2.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMember_roleGecersizse_businessHatasi() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMember(10L, req(1L, "WRONG"))
        );
        assertEquals("INVALID_MEMBER_ROLE", ex.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMember_roleBosluksa_default_MEMBER_veYeniKayit() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addMember(10L, req(1L, "   "));

        verify(memberRepository).save(argThat(saved ->
                saved != null
                        && saved.getDepartment() == d
                        && saved.getUser() == u
                        && saved.isActive()
                        && saved.getMemberRole() == DepartmentMemberRole.MEMBER
        ));
    }

    @Test
    void addMember_roleMANAGER_veYeniKayit() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addMember(10L, req(1L, "MANAGER"));

        verify(memberRepository).save(argThat(saved ->
                saved != null
                        && saved.getMemberRole() == DepartmentMemberRole.MANAGER
                        && saved.isActive()
        ));
    }

    @Test
    void addMember_mevcutUyeVarsa_aktifYapipRolGuncellemeli() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);

        DepartmentMember existing = member(d, u, DepartmentMemberRole.MEMBER, false);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addMember(10L, req(1L, "MANAGER"));

        assertTrue(existing.isActive());
        assertEquals(DepartmentMemberRole.MANAGER, existing.getMemberRole());
        verify(memberRepository).save(existing);
    }

    @Test
    void addMember_mevcutUyeVarsa_veMemberRoleNull_ise_default_MEMBER() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);

        DepartmentMember existing = member(d, u, DepartmentMemberRole.MANAGER, false);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDepartmentMemberRequest request = req(1L, null);

        service.addMember(10L, request);

        assertTrue(existing.isActive());
        assertEquals(DepartmentMemberRole.MEMBER, existing.getMemberRole());
        verify(memberRepository).save(existing);
    }

    // listeleme

    @Test
    void listMembers_ok_responseaMaplemeli() {
        Department d = dept(10L, true);
        User u1 = user(1L, Role.AGENT);
        User u2 = user(2L, Role.AGENT);

        DepartmentMember m1 = member(d, u1, DepartmentMemberRole.MEMBER, true);
        DepartmentMember m2 = member(d, u2, DepartmentMemberRole.MANAGER, false);

        when(memberRepository.findAllByDepartmentId(10L)).thenReturn(List.of(m1, m2));

        List<DepartmentMemberResponse> out = service.listMembers(10L);

        assertEquals(2, out.size());

        assertEquals(1L, out.get(0).getUserId());
        assertEquals("u1@test.com", out.get(0).getEmail());
        assertEquals("5551", out.get(0).getPhone());
        assertEquals("MEMBER", out.get(0).getMemberRole());
        assertTrue(out.get(0).isActive());

        assertEquals(2L, out.get(1).getUserId());
        assertEquals("MANAGER", out.get(1).getMemberRole());
        assertFalse(out.get(1).isActive());

        verify(memberRepository).findAllByDepartmentId(10L);
    }

    // çıkarma

    @Test
    void removeMember_uyeYoksa_notFound() {
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.removeMember(10L, 1L)
        );
        assertEquals("DEPARTMENT_MEMBER_NOT_FOUND", ex.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    void removeMember_ok_pasifYapmali() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);
        DepartmentMember m = member(d, u, DepartmentMemberRole.MEMBER, true);

        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(m));
        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.removeMember(10L, 1L);

        assertFalse(m.isActive());
        verify(memberRepository).save(m);
    }

    // rol değiştirme

    @Test
    void changeRole_uyeYoksa_notFound() {
        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.changeRole(10L, 1L, "MANAGER")
        );
        assertEquals("DEPARTMENT_MEMBER_NOT_FOUND", ex.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    void changeRole_gecersizRol_businessHatasi() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);
        DepartmentMember m = member(d, u, DepartmentMemberRole.MEMBER, true);

        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(m));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeRole(10L, 1L, "NOPE")
        );
        assertEquals("INVALID_MEMBER_ROLE", ex.getMessage());

        verify(memberRepository, never()).save(any());
        assertEquals(DepartmentMemberRole.MEMBER, m.getMemberRole());
    }

    @Test
    void changeRole_blankIse_default_MEMBER() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);
        DepartmentMember m = member(d, u, DepartmentMemberRole.MANAGER, true);

        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(m));
        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeRole(10L, 1L, "   ");

        assertEquals(DepartmentMemberRole.MEMBER, m.getMemberRole());
        verify(memberRepository).save(m);
    }

    @Test
    void changeRole_bosluktanTrimleyip_MANAGER_uygulamali() {
        Department d = dept(10L, true);
        User u = user(1L, Role.AGENT);
        DepartmentMember m = member(d, u, DepartmentMemberRole.MEMBER, true);

        when(memberRepository.findByDepartmentIdAndUserId(10L, 1L)).thenReturn(Optional.of(m));
        when(memberRepository.save(any(DepartmentMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeRole(10L, 1L, "   MANAGER   ");

        assertEquals(DepartmentMemberRole.MANAGER, m.getMemberRole());
        verify(memberRepository).save(m);
    }
}
