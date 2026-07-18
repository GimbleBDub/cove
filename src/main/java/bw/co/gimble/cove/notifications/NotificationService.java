package bw.co.gimble.cove.notifications;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.notifications.dto.NotificationResponse;
import bw.co.gimble.cove.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Internal write path - called by NotificationEventListener, never directly by a controller. */
    public Notification create(UUID userId, NotificationType type, String title, String message,
                               String referenceType, UUID referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications() {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId())
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(currentUserId());
    }

    public NotificationResponse markRead(UUID id) {
        UUID userId = currentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        return map(notification);
    }

    public int markAllRead() {
        UUID userId = currentUserId();
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        Instant now = Instant.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        return unread.size();
    }

    private UUID currentUserId() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthPrincipal principal) {
            return principal.getUserId();
        }
        throw ApiException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Not authenticated");
    }

    private NotificationResponse map(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getReferenceType(), n.getReferenceId(), n.isRead(), n.getReadAt(), n.getCreatedAt()
        );
    }
}