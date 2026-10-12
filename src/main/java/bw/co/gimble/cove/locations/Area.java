package bw.co.gimble.cove.locations;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** An area, suburb, block or ward inside a [City]. */
@Getter
@Setter
@Entity
@Table(name = "areas", uniqueConstraints = @UniqueConstraint(name = "uq_area_city_name", columnNames = {"city_id", "name"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Area extends Auditable {

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private boolean active = true;
}
