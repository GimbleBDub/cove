package bw.co.gimble.cove.notifications.dto;

import bw.co.gimble.cove.notifications.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {}