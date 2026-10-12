package bw.co.gimble.cove.listings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Optional<Listing> findByPropertyIdAndPurpose(UUID propertyId, ListingPurpose purpose);

    List<Listing> findByPropertyId(UUID propertyId);

    long countByPropertyIdAndPublishedTrueAndStatus(UUID propertyId, ListingStatus status);

    List<Listing> findByPublishedTrueAndStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findByPropertyIdIn(java.util.Collection<UUID> propertyIds);

    List<Listing> findByListedByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByStatus(ListingStatus status);
}
