package bw.co.gimble.cove.applications;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "listing_applications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingApplication extends Auditable {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "applicant_user_id", nullable = false)
    private UUID applicantUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    private Integer occupants;

    @Column(precision = 14, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(precision = 14, scale = 2)
    private BigDecimal offerAmount;

    @Column(length = 2000)
    private String conditions;

    private String referralSource;

    private LocalDate proposedStartDate;

    private LocalDate proposedEndDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal proposedRent;

    @Column(precision = 14, scale = 2)
    private BigDecimal proposedDeposit;
}
