package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.notification.NotificationItemResponse;
import com.ademarli.municipality_service.service.CitizenNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citizen/notifications")
public class CitizenNotificationController {

    private final CitizenNotificationService service;

    public CitizenNotificationController(CitizenNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationItemResponse>> myNotifications(
            @AuthenticationPrincipal UserDetails ud,
            Pageable pageable
    ) {
        Long userId = Long.parseLong(ud.getUsername());
        return ResponseEntity.ok(service.list(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        Long userId = Long.parseLong(ud.getUsername());
        return ResponseEntity.ok(service.unreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails ud, @PathVariable Long id) {
        Long userId = Long.parseLong(ud.getUsername());
        service.markRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Integer> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        Long userId = Long.parseLong(ud.getUsername());
        int updated = service.markAllRead(userId);
        return ResponseEntity.ok(updated);
    }
}
