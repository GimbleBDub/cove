package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A redeemable code that grants a plan for free, for a set duration, to whoever redeems
 * it. Platform admins mint these (e.g. "LAUNCH2026") for promos, partners, or support
 * cases - redemption itself is self-service by any user via POST /subscriptions/redeem.
 */
@Getter
@Setter
@Entity
@Table(name = "free_access_codes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeAccessCode extends Auditable {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "plan_id", nullable = false)
    private java.util.UUID planId;

    /** How long the granted subscription lasts once redeemed. Null = never expires. */
    private Integer durationDays;

    /** Null = unlimited redemptions. */
    private Integer maxRedemptions;

    @Column(nullable = false)
    @Builder.Default
    private int redemptionCount = 0;

    /** The code itself stops being redeemable after this instant, independent of maxRedemptions. Null = no deadline. */
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Internal note for admins - "Q3 agency partner promo", etc. Never shown to the redeemer. */
    private String note;
}
