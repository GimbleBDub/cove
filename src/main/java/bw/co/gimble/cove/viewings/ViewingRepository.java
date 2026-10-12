package bw.co.gimble.cove.viewings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ViewingRepository extends JpaRepository<Viewing, UUID> {
    List<Viewing> findByListingIdOrderByStartsAtDesc(UUID listingId);
    List<Viewing> findByRequestedByUserIdOrderByStartsAtDesc(UUID userId);
    List<Viewing> findByLeadIdOrderByStartsAtDesc(UUID leadId);
}
