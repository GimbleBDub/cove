package bw.co.gimble.cove.listings;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "listings", uniqueConstraints = @UniqueConstraint(
        name = "uq_listing_property_purpose", columnNames = {"property_id", "purpose"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing extends Auditable {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.UNPUBLISHED;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "BWP";

    @Enumerated(EnumType.STRING)
    private RentPeriod rentPeriod;

    @Column(length = 4000)
    private String copy;

    private Instant featuredUntil;

    @Column(name = "listed_by_user_id", nullable = false)
    private UUID listedByUserId;

    /** Moderator's reason when rejecting or taking down. */
    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
