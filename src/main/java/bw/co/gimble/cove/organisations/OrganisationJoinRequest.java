package bw.co.gimble.cove.organisations;

import bw.co.gimble.cove.common.Auditable;
import bw.co.gimble.cove.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's request to join an organisation. Joining is never automatic - an
 * AGENCY_ADMIN (or PLATFORM_ADMIN) of the target organisation must approve it,
 * at which point a UserRole row is created for the requester.
 */
@Getter
@Setter
@Entity
@Table(name = "organisation_join_requests")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationJoinRequest extends Auditable {

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Optional hint from the requester about which role they're expecting (e.g. AGENT). */
    @Enumerated(EnumType.STRING)
    private Role requestedRole;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    private UUID reviewedByUserId;

    private Instant reviewedAt;

    private String rejectionReason;
}