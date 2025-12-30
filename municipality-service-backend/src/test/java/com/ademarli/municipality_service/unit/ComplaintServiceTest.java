package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.complaint.*;
import com.ademarli.municipality_service.model.entity.*;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.*;
import com.ademarli.municipality_service.service.ComplaintService;
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
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock UserRepository userRepository;
    @Mock ComplaintCategoryRepository categoryRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock DepartmentMemberRepository departmentMemberRepository;

    @InjectMocks ComplaintService service;

    private User user(Long id, Role... roles) {
        User u = new User();
        u.setId(id);
        if (roles == null) {
            u.setRoles(null);
        } else {
            u.setRoles(new HashSet<>(Arrays.asList(roles)));
        }
        u.setEnabled(true);
        u.setEmail("u" + id + "@mail.com");
        u.setPhone("555" + id);
        return u;
    }

    private Department dept(Long id, boolean active, String name) {
        Department d = new Department();
        d.setId(id);
        d.setActive(active);
        d.setName(name);
        return d;
    }

    private ComplaintCategory category(Long id, boolean active, Department defaultDept, String name) {
        ComplaintCategory c = new ComplaintCategory();
        c.setId(id);
        c.setActive(active);
        c.setDefaultDepartment(defaultDept);
        c.setName(name);
        return c;
    }

    private Complaint complaint(Long id, ComplaintStatus status, Department d, User createdBy) {
        Complaint c = new Complaint();
        c.setId(id);
        c.setStatus(status);
        c.setDepartment(d);
        c.setCreatedBy(createdBy);
        c.setTrackingCode("TRK-AAAA1111");
        c.setTitle("t");
        c.setDescription("desc");
        c.setCreatedAt(OffsetDateTime.now());
        return c;
    }

    // oluşturma

    @Test
    void createComplaint_ok_shouldSaveComplaint_history_andNotification() {
        Long userId = 10L;

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(5L);
        req.setTitle("  Başlık  ");
        req.setDescription("  Açıklama  ");
        req.setLat(41.0);
        req.setLon(28.0);

        User u = user(userId, Role.CITIZEN);
        Department d = dept(2L, true, "Temizlik İşleri");
        ComplaintCategory cat = category(5L, true, d, "Çöp Toplama");

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
        when(complaintRepository.existsByTrackingCode(anyString())).thenReturn(false);

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> {
            Complaint saved = inv.getArgument(0);
            saved.setId(123L); // notification link için gerekli
            return saved;
        });

        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        ComplaintDetailResponse out = service.createComplaint(userId, req);

        assertNotNull(out);
        assertEquals(ComplaintStatus.NEW, out.getStatus());

        verify(complaintRepository).save(argThat(c ->
                c.getCreatedBy() == u &&
                        c.getCategory() == cat &&
                        c.getDepartment() == d &&
                        c.getStatus() == ComplaintStatus.NEW &&
                        c.getTrackingCode() != null &&
                        c.getTrackingCode().startsWith("TRK-") &&
                        "Başlık".equals(c.getTitle()) &&
                        "Açıklama".equals(c.getDescription()) &&
                        Double.valueOf(41.0).equals(c.getLat()) &&
                        Double.valueOf(28.0).equals(c.getLon()) &&
                        c.getUpdatedAt() != null
        ));

        verify(statusHistoryRepository).save(argThat(sh ->
                sh.getComplaint() != null &&
                        sh.getComplaint().getId() != null &&
                        sh.getFromStatus() == null &&
                        sh.getToStatus() == ComplaintStatus.NEW &&
                        sh.getChangedBy() == u &&
                        sh.getNote() != null &&
                        sh.getNote().contains("Şikayetiniz alınmıştır")
        ));

        verify(notificationRepository).save(argThat(n ->
                Objects.equals(n.getUserId(), u.getId()) &&
                        Objects.equals(n.getComplaintId(), 123L) &&
                        "Şikayetiniz alındı".equals(n.getTitle()) &&
                        n.getLink() != null &&
                        n.getLink().equals("/complaints/123")
        ));
    }

    @Test
    void createComplaint_userNotFound_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        assertThrows(NotFoundException.class, () -> service.createComplaint(10L, req));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createComplaint_categoryNotFound_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.CITIZEN)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        assertThrows(NotFoundException.class, () -> service.createComplaint(10L, req));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createComplaint_categoryNotActive_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.CITIZEN)));

        Department d = dept(2L, true, "Temizlik İşleri");
        ComplaintCategory cat = category(1L, false, d, "X");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createComplaint(10L, req));
        assertEquals("CATEGORY_NOT_ACTIVE", ex.getMessage());

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createComplaint_defaultDepartmentNull_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.CITIZEN)));

        ComplaintCategory cat = new ComplaintCategory();
        cat.setId(1L);
        cat.setActive(true);
        cat.setDefaultDepartment(null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createComplaint(10L, req));
        assertEquals("CATEGORY_HAS_NO_DEFAULT_DEPARTMENT", ex.getMessage());

        verify(complaintRepository, never()).save(any());
    }

    @Test
    void createComplaint_defaultDepartmentNotActive_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.CITIZEN)));

        Department d = dept(2L, false, "Temizlik İşleri");
        ComplaintCategory cat = category(1L, true, d, "X");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createComplaint(10L, req));
        assertEquals("DEFAULT_DEPARTMENT_NOT_ACTIVE", ex.getMessage());

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createComplaint_trackingCodeGenerationFailsAfterRetries_shouldThrowIllegalState() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.CITIZEN)));

        Department d = dept(2L, true, "Temizlik İşleri");
        ComplaintCategory cat = category(1L, true, d, "X");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        // 5 denemeyi de "exists=true" yapınca fail olmalı
        when(complaintRepository.existsByTrackingCode(anyString())).thenReturn(true);

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCategoryId(1L);

        assertThrows(IllegalStateException.class, () -> service.createComplaint(10L, req));
        verify(complaintRepository, never()).save(any());
    }

    // listeleme

    @Test
    void listMyComplaints_ok_shouldMapToSummary() {
        Long userId = 10L;
        User u = user(userId, Role.CITIZEN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        Department d = dept(2L, true, "Temizlik İşleri");

        Complaint c1 = complaint(1L, ComplaintStatus.NEW, d, u);
        c1.setTrackingCode("TRK-1111");
        c1.setTitle("A");

        Complaint c2 = complaint(2L, ComplaintStatus.IN_REVIEW, d, u);
        c2.setTrackingCode("TRK-2222");
        c2.setTitle("B");

        when(complaintRepository.findByCreatedBy(u)).thenReturn(List.of(c1, c2));

        List<ComplaintSummaryResponse> out = service.listMyComplaints(userId);

        assertEquals(2, out.size());
        assertEquals("TRK-1111", out.get(0).getTrackingCode());
        assertEquals("A", out.get(0).getTitle());
        assertEquals(ComplaintStatus.NEW, out.get(0).getStatus());

        verify(complaintRepository).findByCreatedBy(u);
    }

    @Test
    void listMyComplaints_userNotFound_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.listMyComplaints(10L));

        verify(complaintRepository, never()).findByCreatedBy(any());
    }

    // sahiplik kontrolü

    @Test
    void getMyComplaintOrThrow_ok_owner() {
        User owner = user(10L, Role.CITIZEN);
        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), owner);

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        ComplaintDetailResponse out = service.getMyComplaintOrThrow(1L, 10L);

        assertEquals(1L, out.getId());
        assertEquals(ComplaintStatus.NEW, out.getStatus());
    }

    @Test
    void getMyComplaintOrThrow_notFound_shouldThrow() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getMyComplaintOrThrow(1L, 10L));
    }

    @Test
    void getMyComplaintOrThrow_notOwner_shouldThrowAccessDenied() {
        User owner = user(10L, Role.CITIZEN);
        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), owner);

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getMyComplaintOrThrow(1L, 999L));
    }

    // durum değiştirme

    @Test
    void changeStatus_actorNotFound_shouldThrow() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.changeStatus(5L, 1L, ComplaintStatus.IN_REVIEW, "note", null));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void changeStatus_complaintNotFound_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, Role.ADMIN)));
        when(complaintRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.changeStatus(10L, 1L, ComplaintStatus.IN_REVIEW, "note", null));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void changeStatus_actorNotStaff_shouldThrowAccessDenied() {
        User actor = user(100L, Role.CITIZEN);
        User citizen = user(10L, Role.CITIZEN);
        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.IN_REVIEW, "note", null));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void changeStatus_agentNotDepartmentMember_shouldThrowAccessDenied() {
        User actor = user(100L, Role.AGENT);
        User citizen = user(10L, Role.CITIZEN);
        Department d = dept(2L, true, "Temizlik İşleri");
        Complaint c = complaint(1L, ComplaintStatus.NEW, d, citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));
        when(departmentMemberRepository.existsByDepartmentIdAndUserIdAndActiveTrue(2L, 100L)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.IN_REVIEW, "note", null));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void changeStatus_invalidTransition_NEW_to_RESOLVED_shouldThrow() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);
        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.RESOLVED, "note", "answer"));

        assertTrue(ex.getMessage().startsWith("INVALID_STATUS_TRANSITION"));

        verify(complaintRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void changeStatus_publicAnswerProvidedButNotResolved_shouldThrow() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);
        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.IN_REVIEW, "note", "answer"));

        assertEquals("PUBLIC_ANSWER_ONLY_ALLOWED_ON_RESOLVED", ex.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void changeStatus_to_IN_REVIEW_shouldSaveHistory_andNotify() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);

        Complaint c = complaint(1L, ComplaintStatus.NEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(100L, 1L, ComplaintStatus.IN_REVIEW, " inceliyoruz ", null);

        verify(complaintRepository).save(argThat(x ->
                x.getStatus() == ComplaintStatus.IN_REVIEW &&
                        x.getUpdatedAt() != null
        ));

        verify(statusHistoryRepository).save(argThat(sh ->
                sh.getFromStatus() == ComplaintStatus.NEW &&
                        sh.getToStatus() == ComplaintStatus.IN_REVIEW &&
                        sh.getChangedBy() == actor &&
                        " inceliyoruz ".equals(sh.getNote())
        ));

        verify(notificationRepository).save(argThat(n ->
                "Şikayetiniz incelenmeye alındı".equals(n.getTitle()) &&
                        n.getBody() != null &&
                        n.getBody().contains("incelenmeye alınmıştır") &&
                        Objects.equals(n.getComplaintId(), 1L) &&
                        Objects.equals(n.getUserId(), citizen.getId()) &&
                        "/complaints/1".equals(n.getLink())
        ));
    }

    @Test
    void changeStatus_to_RESOLVED_requiresPublicAnswer_shouldThrow() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);

        Complaint c = complaint(1L, ComplaintStatus.IN_REVIEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.RESOLVED, "note", null));

        assertEquals("PUBLIC_ANSWER_REQUIRED_ON_RESOLVED", ex.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void changeStatus_to_RESOLVED_ok_shouldSetResolvedAt_publicAnswer_andNotify() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);

        Complaint c = complaint(1L, ComplaintStatus.IN_REVIEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(100L, 1L, ComplaintStatus.RESOLVED, "note", "  Çözüldü.  ");

        verify(complaintRepository).save(argThat(x ->
                x.getStatus() == ComplaintStatus.RESOLVED &&
                        x.getResolvedAt() != null &&
                        "Çözüldü.".equals(x.getPublicAnswer()) &&
                        x.getUpdatedAt() != null
        ));

        verify(notificationRepository).save(argThat(n ->
                "Şikayetiniz çözüldü".equals(n.getTitle()) &&
                        "Çözüldü.".equals(n.getBody()) &&
                        Objects.equals(n.getComplaintId(), 1L) &&
                        Objects.equals(n.getUserId(), citizen.getId()) &&
                        "/complaints/1".equals(n.getLink())
        ));
    }

    @Test
    void changeStatus_to_CLOSED_ok_shouldSetClosedAt_andNotifyBodyContainsNote() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);

        Complaint c = complaint(1L, ComplaintStatus.IN_REVIEW, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(100L, 1L, ComplaintStatus.CLOSED, "Eksik bilgi", null);

        verify(complaintRepository).save(argThat(x ->
                x.getStatus() == ComplaintStatus.CLOSED &&
                        x.getClosedAt() != null &&
                        x.getUpdatedAt() != null
        ));

        verify(notificationRepository).save(argThat(n ->
                "Şikayetiniz kapatıldı".equals(n.getTitle()) &&
                        n.getBody() != null &&
                        n.getBody().contains("Eksik bilgi") &&
                        Objects.equals(n.getComplaintId(), 1L) &&
                        Objects.equals(n.getUserId(), citizen.getId())
        ));
    }

    @Test
    void changeStatus_alreadyClosed_shouldThrow() {
        User actor = user(100L, Role.ADMIN);
        User citizen = user(10L, Role.CITIZEN);

        Complaint c = complaint(1L, ComplaintStatus.CLOSED, dept(2L, true, "Temizlik İşleri"), citizen);

        when(userRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.changeStatus(100L, 1L, ComplaintStatus.CLOSED, "note", null));

        assertEquals("COMPLAINT_ALREADY_CLOSED", ex.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    // public tracking

    @Test
    void trackByCode_ok_shouldReturnPublicTrackingResponse() {
        User citizen = user(10L, Role.CITIZEN);
        Department d = dept(2L, true, "Temizlik İşleri");
        Complaint c = complaint(1L, ComplaintStatus.NEW, d, citizen);
        c.setTrackingCode("TRK-XYZ");

        when(complaintRepository.findByTrackingCode("TRK-XYZ")).thenReturn(Optional.of(c));

        Optional<PublicTrackingResponse> out = service.trackByCode("TRK-XYZ");

        assertTrue(out.isPresent());
        assertEquals("TRK-XYZ", out.get().getTrackingCode());
        assertEquals(ComplaintStatus.NEW, out.get().getStatus());
        assertEquals("Temizlik İşleri", out.get().getDepartmentName());
    }

    @Test
    void trackByCode_notFound_shouldReturnEmpty() {
        when(complaintRepository.findByTrackingCode("TRK-NO")).thenReturn(Optional.empty());

        assertTrue(service.trackByCode("TRK-NO").isEmpty());
    }

    // public feed

    @Test
    void publicFeed_ok_shouldMapAvgAndCount_andMaskTrackingCode() {
        ComplaintRepository.PublicFeedRow row = mock(ComplaintRepository.PublicFeedRow.class);

        when(row.getId()).thenReturn(5L);
        when(row.getTrackingCode()).thenReturn("TRK-ABCDEFGH");
        when(row.getTitle()).thenReturn("Başlık");
        when(row.getCategoryName()).thenReturn("Gürültü");
        when(row.getDepartmentName()).thenReturn("Zabıta");
        when(row.getStatus()).thenReturn(ComplaintStatus.RESOLVED);
        when(row.getAnsweredAt()).thenReturn(OffsetDateTime.now());
        when(row.getPublicAnswer()).thenReturn("Yanıt");
        when(row.getAvgRating()).thenReturn(4.5);
        when(row.getRatingCount()).thenReturn(12L);

        Page<ComplaintRepository.PublicFeedRow> page = new PageImpl<>(
                List.of(row),
                PageRequest.of(0, 10),
                1
        );

        when(complaintRepository.publicFeedResolvedWithRatings(any(Pageable.class))).thenReturn(page);

        Page<PublicFeedItem> out = service.publicFeed(PageRequest.of(0, 10));

        assertEquals(1, out.getTotalElements());
        PublicFeedItem item = out.getContent().get(0);

        assertEquals(5L, item.getId());
        assertEquals("Başlık", item.getTitle());
        assertEquals(ComplaintStatus.RESOLVED, item.getStatus());
        assertEquals(4.5, item.getAvgRating());
        assertEquals(12L, item.getRatingCount());

        assertEquals("TRK****GH", item.getTrackingCode());
    }

    @Test
    void publicFeed_nullAvgCount_shouldDefaultToZero() {
        ComplaintRepository.PublicFeedRow row = mock(ComplaintRepository.PublicFeedRow.class);

        when(row.getId()).thenReturn(5L);
        when(row.getTrackingCode()).thenReturn("TRK-ABCDEFGH");
        when(row.getTitle()).thenReturn("Başlık");
        when(row.getStatus()).thenReturn(ComplaintStatus.RESOLVED);
        when(row.getAvgRating()).thenReturn(null);
        when(row.getRatingCount()).thenReturn(null);

        Page<ComplaintRepository.PublicFeedRow> page = new PageImpl<>(
                List.of(row),
                PageRequest.of(0, 10),
                1
        );

        when(complaintRepository.publicFeedResolvedWithRatings(any(Pageable.class))).thenReturn(page);

        PublicFeedItem item = service.publicFeed(PageRequest.of(0, 10)).getContent().get(0);

        assertEquals(0d, item.getAvgRating());
        assertEquals(0L, item.getRatingCount());
    }
}
