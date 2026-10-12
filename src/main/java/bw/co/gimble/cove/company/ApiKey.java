package bw.co.gimble.cove.company;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A company's credential for the integration API. Only a hash of the secret is
 * stored; the full key is shown once when created. Calls act as [createdByUserId].
 */
@Getter
@Setter
@Entity
@Table(name = "api_keys", uniqueConstraints = @UniqueConstraint(name = "uq_api_key_prefix", columnNames = "prefix"))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends Auditable {

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String name;

    /** Public part of the key, used to look it up: cove_live_{prefix}_{secret}. */
    @Column(nullable = false, length = 16)
    private String prefix;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
