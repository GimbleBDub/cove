package bw.co.gimble.cove.applications;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "leases")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lease extends Auditable {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "tenant_user_id", nullable = false)
    private UUID tenantUserId;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal rentAmount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "BWP";

    @Column(precision = 14, scale = 2)
    private BigDecimal depositAmount;
}
