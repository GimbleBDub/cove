package bw.co.gimble.cove.viewings;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "viewings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viewing extends Auditable {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ViewingStatus status = ViewingStatus.REQUESTED;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "confirmed_by_user_id")
    private UUID confirmedByUserId;
}
