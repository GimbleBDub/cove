package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "property_owners", uniqueConstraints = @UniqueConstraint(
        name = "uq_property_owner", columnNames = {"property_id", "user_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyOwner extends Auditable {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal splitPercent;
}
