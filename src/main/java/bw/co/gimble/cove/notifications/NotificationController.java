package bw.co.gimble.cove.notifications;

import bw.co.gimble.cove.notifications.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unreadCount", notificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("markedRead", notificationService.markAllRead());
    }
}