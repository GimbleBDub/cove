package bw.co.gimble.cove.applications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignatureEventRepository extends JpaRepository<SignatureEvent, UUID> {
    List<SignatureEvent> findByApplicationId(UUID applicationId);
    boolean existsByApplicationIdAndSignerUserIdAndParty(UUID applicationId, UUID signerUserId, String party);
}
