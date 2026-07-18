package bw.co.gimble.cove.notifications;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends Auditable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    /** Loosely typed pointer back to the entity this notification is about, e.g. "ORGANISATION". */
    private String referenceType;

    private UUID referenceId;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    private Instant readAt;
}
