package bw.co.gimble.cove.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, UUID> {
    List<PropertyOwner> findByPropertyId(UUID propertyId);
    boolean existsByPropertyIdAndUserId(UUID propertyId, UUID userId);
    Optional<PropertyOwner> findByPropertyIdAndUserId(UUID propertyId, UUID userId);
    void deleteByPropertyId(UUID propertyId);
}
