package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "property_media")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyMedia extends Auditable {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private String storageKey;

    private String url;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /** IMAGE or VIDEO. Null on rows created before videos were supported - treated as IMAGE. */
    @Column(name = "media_type")
    private String mediaType;

    /** Set when imported by URL through the integration API, so re-syncs don't download it again. */
    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    public boolean isVideo() {
        return "VIDEO".equals(mediaType);
    }
}
