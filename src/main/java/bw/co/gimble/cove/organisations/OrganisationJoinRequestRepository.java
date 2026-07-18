package bw.co.gimble.cove.organisations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationJoinRequestRepository extends JpaRepository<OrganisationJoinRequest, UUID> {

    boolean existsByOrganisationIdAndUserIdAndStatus(UUID organisationId, UUID userId, JoinRequestStatus status);

    Optional<OrganisationJoinRequest> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<OrganisationJoinRequest> findByOrganisationIdAndStatus(UUID organisationId, JoinRequestStatus status);

    List<OrganisationJoinRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
}