package bw.co.gimble.cove.locations;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

/** A city, town or village people can list in. Managed by Cove staff (CATALOGUE). */
@Getter
@Setter
@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(name = "uq_city_name", columnNames = "name"))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City extends Auditable {

    @Column(nullable = false)
    private String name;

    /** Administrative district, e.g. South-East, North-East. Optional. */
    private String district;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private boolean active = true;
}
