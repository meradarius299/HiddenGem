package com.hiddengems.controller;

import com.hiddengems.dto.NotificationResponse;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        return notificationService.list(auth.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationService.unreadCount(auth.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markRead(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
