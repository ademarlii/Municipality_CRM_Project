package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.agent.AgentComplaintListItem;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.service.AgentComplaintService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock DepartmentMemberRepository departmentMemberRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AgentComplaintService service;

    private User user(Long id, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setEnabled(true);
        if (roles == null) u.setRoles(null);
        else u.setRoles(new HashSet<>(Arrays.asList(roles)));
        return u;
    }

    private Department dept(Long id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private ComplaintCategory category(Long id, String name) {
        ComplaintCategory c = new ComplaintCategory();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private Complaint complaint(Long id,
                                String tracking,
                                String title,
                                ComplaintStatus status,
                                OffsetDateTime createdAt,
                                ComplaintCategory category,
                                Department dept,
                                User createdBy) {
        Complaint c = new Complaint();
        c.setId(id);
        c.setTrackingCode(tracking);
        c.setTitle(title);
        c.setStatus(status);
        c.setCreatedAt(createdAt);
        c.setCategory(category);
        c.setDepartment(dept);
        c.setCreatedBy(createdBy);
        return c;
    }

    // korumalar

    @Test
    void listForAgent_userYoksa_notFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.listForAgent(10L, null, null, PageRequest.of(0, 10))
        );
        assertEquals("USER_NOT_FOUND", ex.getMessage());

        verify(userRepository).findById(10L);
        verify(departmentMemberRepository, never()).findActiveDepartmentIdsByUserId(anyLong());
        verify(complaintRepository, never()).findAgentList(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    void listForAgent_rolesNull_ise_businessHatasi() {
        User agent = user(10L, (Role[]) null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.listForAgent(10L, "x", List.of(ComplaintStatus.NEW), PageRequest.of(0, 10))
        );
        assertEquals("ONLY_STAFF_CAN_CHANGE_STATUS", ex.getMessage());

        verify(userRepository).findById(10L);
        verify(departmentMemberRepository, never()).findActiveDepartmentIdsByUserId(anyLong());
        verify(complaintRepository, never()).findAgentList(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    void listForAgent_agentDegilse_businessHatasi() {
        User citizen = user(10L, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.listForAgent(10L, "x", List.of(ComplaintStatus.NEW), PageRequest.of(0, 10))
        );
        assertEquals("ONLY_STAFF_CAN_CHANGE_STATUS", ex.getMessage());

        verify(userRepository).findById(10L);
        verify(departmentMemberRepository, never()).findActiveDepartmentIdsByUserId(anyLong());
        verify(complaintRepository, never()).findAgentList(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    void listForAgent_deptIdsNull_ise_businessHatasi() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.listForAgent(10L, "x", List.of(ComplaintStatus.NEW), PageRequest.of(0, 10))
        );
        assertEquals("NOT_A_MEMBER_OF_THIS_DEPARTMENT", ex.getMessage());

        verify(departmentMemberRepository).findActiveDepartmentIdsByUserId(10L);
        verify(complaintRepository, never()).findAgentList(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    void listForAgent_deptIdsBos_ise_businessHatasi() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.listForAgent(10L, "x", List.of(ComplaintStatus.NEW), PageRequest.of(0, 10))
        );
        assertEquals("NOT_A_MEMBER_OF_THIS_DEPARTMENT", ex.getMessage());

        verify(departmentMemberRepository).findActiveDepartmentIdsByUserId(10L);
        verify(complaintRepository, never()).findAgentList(anyList(), any(), any(), any(Pageable.class));
    }

    // query normalize + repo çağrısı

    @Test
    void listForAgent_qNull_ise_repoYaNullGitmeli() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(7L, 8L));

        PageRequest pageable = PageRequest.of(0, 10);
        when(complaintRepository.findAgentList(anyList(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AgentComplaintListItem> out = service.listForAgent(10L, null, null, pageable);

        assertNotNull(out);
        assertEquals(0, out.getTotalElements());

        verify(complaintRepository).findAgentList(eq(List.of(7L, 8L)), isNull(), isNull(), eq(pageable));
    }

    @Test
    void listForAgent_qBosluksa_trimSonrasiNullOlmali() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(7L, 8L));

        PageRequest pageable = PageRequest.of(1, 5);
        when(complaintRepository.findAgentList(anyList(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listForAgent(10L, "   ", null, pageable);

        verify(complaintRepository).findAgentList(eq(List.of(7L, 8L)), isNull(), isNull(), eq(pageable));
    }

    @Test
    void listForAgent_qTrimlenip_repoYaGitmeli_veStatusesAyniGitmeli() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(3L));

        List<ComplaintStatus> statuses = List.of(ComplaintStatus.NEW, ComplaintStatus.IN_REVIEW);
        PageRequest pageable = PageRequest.of(0, 10);

        when(complaintRepository.findAgentList(anyList(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listForAgent(10L, "  TRK-123  ", statuses, pageable);

        verify(complaintRepository).findAgentList(
                eq(List.of(3L)),
                argThat(s -> "TRK-123".equals(s)),
                eq(statuses),
                eq(pageable)
        );
    }

    @Test
    void listForAgent_repoExceptionAtilirsa_aynenFirlatmali() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(1L));

        RuntimeException boom = new RuntimeException("db down");
        when(complaintRepository.findAgentList(anyList(), any(), any(), any(Pageable.class)))
                .thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.listForAgent(10L, "x", List.of(ComplaintStatus.NEW), PageRequest.of(0, 10))
        );

        assertSame(boom, ex);
    }

    // mapping

    @Test
    void listForAgent_ok_tumAlanlariMaplemeli() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(2L, 5L));

        OffsetDateTime now = OffsetDateTime.now();

        User citizen = new User();
        citizen.setId(99L);
        citizen.setEmail("citizen@mail.com");

        Department d = dept(2L, "Zabıta");
        ComplaintCategory cat = category(7L, "Gürültü");

        Complaint c1 = complaint(1L, "TRK-111", "Başlık1", ComplaintStatus.NEW, now, cat, d, citizen);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Complaint> page = new PageImpl<>(List.of(c1), pageable, 1);

        when(complaintRepository.findAgentList(eq(List.of(2L, 5L)), eq("q"), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<AgentComplaintListItem> out = service.listForAgent(10L, "q", null, pageable);

        assertEquals(1, out.getTotalElements());
        AgentComplaintListItem item = out.getContent().get(0);

        assertEquals(1L, item.getId());
        assertEquals("TRK-111", item.getTrackingCode());
        assertEquals("Başlık1", item.getTitle());
        assertEquals(ComplaintStatus.NEW, item.getStatus());
        assertEquals(now, item.getCreatedAt());
        assertEquals("Gürültü", item.getCategoryName());
        assertEquals("Zabıta", item.getDepartmentName());
        assertEquals("citizen@mail.com", item.getCitizenEmail());
    }

    @Test
    void listForAgent_category_dept_createdBy_nullIse_nullMaplemeli() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(1L));

        OffsetDateTime now = OffsetDateTime.now();
        Complaint c = complaint(1L, "TRK-X", "T", ComplaintStatus.IN_REVIEW, now, null, null, null);

        PageRequest pageable = PageRequest.of(0, 10);
        when(complaintRepository.findAgentList(eq(List.of(1L)), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(c), pageable, 1));

        Page<AgentComplaintListItem> out = service.listForAgent(10L, null, null, pageable);

        assertEquals(1, out.getTotalElements());
        AgentComplaintListItem item = out.getContent().get(0);

        assertEquals(1L, item.getId());
        assertEquals("TRK-X", item.getTrackingCode());
        assertEquals("T", item.getTitle());
        assertEquals(ComplaintStatus.IN_REVIEW, item.getStatus());
        assertEquals(now, item.getCreatedAt());

        assertNull(item.getCategoryName());
        assertNull(item.getDepartmentName());
        assertNull(item.getCitizenEmail());
    }

    // çoklu senaryo (paging + statuses + mapping)

    @Test
    void listForAgent_cokluKayit_paging_veStatuses_calismali() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(1L, 2L));

        OffsetDateTime t1 = OffsetDateTime.now().minusDays(1);
        OffsetDateTime t2 = OffsetDateTime.now();

        User citizen1 = new User();
        citizen1.setEmail("c1@mail.com");
        Department d1 = dept(1L, "Temizlik");
        ComplaintCategory cat1 = category(1L, "Çöp");

        User citizen2 = new User();
        citizen2.setEmail("c2@mail.com");
        Department d2 = dept(2L, "Zabıta");
        ComplaintCategory cat2 = category(2L, "Gürültü");

        Complaint a = complaint(11L, "TRK-AAA", "A", ComplaintStatus.NEW, t1, cat1, d1, citizen1);
        Complaint b = complaint(12L, "TRK-BBB", "B", ComplaintStatus.IN_REVIEW, t2, cat2, d2, citizen2);

        List<ComplaintStatus> statuses = List.of(ComplaintStatus.NEW, ComplaintStatus.IN_REVIEW);

        PageRequest pageable = PageRequest.of(1, 2, Sort.by("createdAt").descending());
        PageImpl<Complaint> page = new PageImpl<>(List.of(a, b), pageable, 5);

        when(complaintRepository.findAgentList(eq(List.of(1L, 2L)), eq("TRK"), eq(statuses), eq(pageable)))
                .thenReturn(page);

        Page<AgentComplaintListItem> out = service.listForAgent(10L, "  TRK  ", statuses, pageable);

        assertEquals(5, out.getTotalElements());
        assertEquals(2, out.getContent().size());
        assertEquals(1, out.getNumber());
        assertEquals(2, out.getSize());

        AgentComplaintListItem i1 = out.getContent().get(0);
        assertEquals(11L, i1.getId());
        assertEquals("TRK-AAA", i1.getTrackingCode());
        assertEquals("A", i1.getTitle());
        assertEquals(ComplaintStatus.NEW, i1.getStatus());
        assertEquals(t1, i1.getCreatedAt());
        assertEquals("Çöp", i1.getCategoryName());
        assertEquals("Temizlik", i1.getDepartmentName());
        assertEquals("c1@mail.com", i1.getCitizenEmail());

        AgentComplaintListItem i2 = out.getContent().get(1);
        assertEquals(12L, i2.getId());
        assertEquals("TRK-BBB", i2.getTrackingCode());
        assertEquals("B", i2.getTitle());
        assertEquals(ComplaintStatus.IN_REVIEW, i2.getStatus());
        assertEquals(t2, i2.getCreatedAt());
        assertEquals("Gürültü", i2.getCategoryName());
        assertEquals("Zabıta", i2.getDepartmentName());
        assertEquals("c2@mail.com", i2.getCitizenEmail());

        verify(complaintRepository).findAgentList(eq(List.of(1L, 2L)), eq("TRK"), eq(statuses), eq(pageable));
    }

    @Test
    void listForAgent_repoBosPageDonerse_bosDonmeli() {
        User agent = user(10L, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(departmentMemberRepository.findActiveDepartmentIdsByUserId(10L)).thenReturn(List.of(10L));

        PageRequest pageable = PageRequest.of(0, 10);
        when(complaintRepository.findAgentList(eq(List.of(10L)), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AgentComplaintListItem> out = service.listForAgent(10L, null, null, pageable);

        assertNotNull(out);
        assertEquals(0, out.getTotalElements());
        assertTrue(out.getContent().isEmpty());

        verify(complaintRepository).findAgentList(eq(List.of(10L)), isNull(), isNull(), eq(pageable));
    }
}
