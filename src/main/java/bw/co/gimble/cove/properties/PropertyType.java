package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "property_types")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyType extends Auditable {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
