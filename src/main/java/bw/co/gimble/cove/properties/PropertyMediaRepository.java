package bw.co.gimble.cove.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, UUID> {
    List<PropertyMedia> findByPropertyIdOrderBySortOrderAscCreatedAtAsc(UUID propertyId);
    long countByPropertyId(UUID propertyId);

    @org.springframework.data.jpa.repository.Query(
            "select count(m) from PropertyMedia m where m.propertyId = :propertyId and (m.mediaType is null or m.mediaType <> 'VIDEO')")
    long countPhotos(@org.springframework.data.repository.query.Param("propertyId") UUID propertyId);
}
