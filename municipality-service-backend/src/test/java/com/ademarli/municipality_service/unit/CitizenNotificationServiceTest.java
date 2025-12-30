package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.model.dto.notification.NotificationItemResponse;
import com.ademarli.municipality_service.model.entity.Notification;
import com.ademarli.municipality_service.repository.NotificationRepository;
import com.ademarli.municipality_service.service.CitizenNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenNotificationServiceTest {

    @Mock NotificationRepository repo;

    @InjectMocks CitizenNotificationService service;

    private Notification n(Long id, Long complaintId, String title, String body, String link, boolean read, OffsetDateTime at) {
        Notification x = new Notification();
        x.setId(id);
        x.setComplaintId(complaintId);
        x.setTitle(title);
        x.setBody(body);
        x.setLink(link);
        x.setRead(read);
        x.setCreatedAt(at);
        return x;
    }

    @Test
    void list_ok_shouldCallRepo_andMapToDto() {
        Long userId = 10L;
        PageRequest pageable = PageRequest.of(0, 10);

        OffsetDateTime t1 = OffsetDateTime.now().minusMinutes(5);
        OffsetDateTime t2 = OffsetDateTime.now();

        Notification n1 = n(1L, 100L, "T1", "B1", "/x/1", false, t1);
        Notification n2 = n(2L, 200L, "T2", "B2", "/x/2", true, t2);

        Page<Notification> page = new PageImpl<>(List.of(n2, n1), pageable, 2);
        when(repo.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<NotificationItemResponse> out = service.list(userId, pageable);

        assertNotNull(out);
        assertEquals(2, out.getTotalElements());
        assertEquals(2, out.getContent().size());

        NotificationItemResponse r0 = out.getContent().get(0);
        assertEquals(2L, r0.getId());
        assertEquals(200L, r0.getComplaintId());
        assertEquals("T2", r0.getTitle());
        assertEquals("B2", r0.getBody());
        assertEquals("/x/2", r0.getLink());
        assertTrue(r0.isRead());
        assertEquals(t2, r0.getCreatedAt());

        NotificationItemResponse r1 = out.getContent().get(1);
        assertEquals(1L, r1.getId());
        assertEquals(100L, r1.getComplaintId());
        assertEquals("T1", r1.getTitle());
        assertEquals("B1", r1.getBody());
        assertEquals("/x/1", r1.getLink());
        assertFalse(r1.isRead());
        assertEquals(t1, r1.getCreatedAt());

        verify(repo).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Test
    void list_emptyPage_shouldReturnEmpty() {
        Long userId = 10L;
        PageRequest pageable = PageRequest.of(0, 10);

        when(repo.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(Page.empty(pageable));

        Page<NotificationItemResponse> out = service.list(userId, pageable);

        assertNotNull(out);
        assertEquals(0, out.getTotalElements());
        assertTrue(out.getContent().isEmpty());

        verify(repo).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Test
    void unreadCount_ok_shouldReturnRepoCount() {
        when(repo.countByUserIdAndIsReadFalse(10L)).thenReturn(7L);

        long out = service.unreadCount(10L);

        assertEquals(7L, out);
        verify(repo).countByUserIdAndIsReadFalse(10L);
    }

    @Test
    void markRead_shouldCallRepoWithCorrectParams() {
        service.markRead(10L, 55L);

        verify(repo).markRead(55L, 10L);
    }

    @Test
    void markAllRead_shouldCallRepo_andReturnUpdatedCount() {
        when(repo.markAllRead(10L)).thenReturn(12);

        int out = service.markAllRead(10L);

        assertEquals(12, out);
        verify(repo).markAllRead(10L);
    }
}
