package bw.co.gimble.cove.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_log_entries")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private String action;

    private String fromValue;

    private String toValue;

    private UUID actorUserId;

    @Column(nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(length = 2000)
    private String note;
}
