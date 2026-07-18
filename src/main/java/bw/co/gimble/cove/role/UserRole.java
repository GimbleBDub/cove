package bw.co.gimble.cove.role;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row = "this user holds this role, optionally at this organisations".
 * A user has many of these. This is the ONLY place access is decided from -
 * User never carries role fields directly.
 */
@Getter
@Setter
@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_role_org",
                columnNames = {"user_id", "role", "organisation_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole extends Auditable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Null for personal-capacity roles (OWNER, TENANT, BUYER). Required otherwise. */
    @Column(name = "organisation_id")
    private UUID organisationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoleStatus status = RoleStatus.ACTIVE;

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    private Instant revokedAt;

    private UUID revokedByUserId;
}