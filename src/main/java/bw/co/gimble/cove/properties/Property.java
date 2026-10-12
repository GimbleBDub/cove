package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "properties")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property extends Auditable {

    @Column(name = "type_id", nullable = false)
    private UUID typeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.UNLISTED;

    @Column(length = 4000)
    private String description;

    private Integer bedrooms;
    private Integer bathrooms;
    private Integer parkingSpaces;

    @Column(precision = 12, scale = 2)
    private BigDecimal sizeSqm;

    @Column(nullable = false)
    @Builder.Default
    private String country = "BW";

    private String city;
    private String suburb;
    private String street;

    /** Catalogue references (locations package); city/suburb above hold their names for display and search. */
    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "area_id")
    private UUID areaId;

    /** The listing company's own reference (from their ERP via the integration API); unique per company. */
    @Column(name = "external_ref")
    private String externalRef;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "managing_organisation_id")
    private UUID managingOrganisationId;
}
