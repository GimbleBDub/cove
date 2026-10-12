package bw.co.gimble.cove.subscriptions;

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
 * One row = a subject (an Organisation OR a User) holding a plan for a period of time.
 * A subject has at most one ACTIVE subscription at once - granting a new one supersedes
 * the previous (see SubscriptionService#assign). Kept intentionally subject-type-agnostic
 * so the same table/service serves org-based and individual-based subscriptions per the
 * "reusable" requirement, rather than two parallel schemas.
 */
@Getter
@Setter
@Entity
@Table(
        name = "subscriptions",
        indexes = @Index(name = "idx_sub_subject", columnList = "subject_type, subject_id, status")
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends Auditable {

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubscriptionSubjectType subjectType;

    /** organisationId when subjectType=ORGANISATION, userId when subjectType=INDIVIDUAL. */
    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionSource source;

    @Column(nullable = false)
    private Instant startsAt;

    /** Null = does not lapse on its own; stays ACTIVE until cancelled. */
    private Instant expiresAt;

    /** Admin who granted it directly, or who created the code that was redeemed. Null if self-service later. */
    private UUID grantedByUserId;

    /** Set only when source=FREE_CODE. */
    private UUID freeAccessCodeId;

    private Instant cancelledAt;

    private UUID cancelledByUserId;

    private String note;
}
