package bw.co.gimble.cove.leads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByListingIdAndInquirerUserId(UUID listingId, UUID inquirerUserId);
    List<Lead> findByListingIdOrderByCreatedAtDesc(UUID listingId);
    List<Lead> findByInquirerUserIdOrderByCreatedAtDesc(UUID inquirerUserId);
    List<Lead> findByAssignedAgentUserIdOrderByCreatedAtDesc(UUID assignedAgentUserId);
}
