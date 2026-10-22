package com.ticketing.notification.controller;

import com.ticketing.auth.security.SessionUser;
import com.ticketing.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/api/notifications/unread-count")
    @PreAuthorize("hasAuthority('notification:read')")
    public UnreadCount count(@AuthenticationPrincipal SessionUser user) {
        return new UnreadCount(service.countUnread(user.getId()));
    }

    public record UnreadCount(long count) { }

    @GetMapping("/api/notifications")
    @PreAuthorize("hasAuthority('notification:read')")
    public java.util.List<com.ticketing.notification.model.NotificationRow> list(@AuthenticationPrincipal SessionUser user,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page) {
        return service.list(user.getId(), page);
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/notifications/{id}/read")
    @PreAuthorize("hasAuthority('notification:mark-read')")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal SessionUser user, @org.springframework.web.bind.annotation.PathVariable Long id) {
        service.read(user.getId(), id);
    }
}
