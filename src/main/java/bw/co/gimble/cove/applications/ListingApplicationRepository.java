package bw.co.gimble.cove.applications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingApplicationRepository extends JpaRepository<ListingApplication, UUID> {
    List<ListingApplication> findByListingIdOrderByCreatedAtDesc(UUID listingId);
    List<ListingApplication> findByApplicantUserIdOrderByCreatedAtDesc(UUID applicantUserId);
}
