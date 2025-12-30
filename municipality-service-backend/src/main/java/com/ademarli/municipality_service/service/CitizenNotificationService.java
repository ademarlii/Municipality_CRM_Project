package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.model.dto.notification.NotificationItemResponse;
import com.ademarli.municipality_service.model.entity.Notification;
import com.ademarli.municipality_service.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitizenNotificationService {

    private final NotificationRepository repo;

    public CitizenNotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public Page<NotificationItemResponse> list(Long userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }

    public long unreadCount(Long userId) {
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        repo.markRead(notificationId, userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repo.markAllRead(userId);
    }

    private NotificationItemResponse toDto(Notification n) {
        NotificationItemResponse r = new NotificationItemResponse();
        r.setId(n.getId());
        r.setComplaintId(n.getComplaintId());
        r.setTitle(n.getTitle());
        r.setBody(n.getBody());
        r.setLink(n.getLink());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
