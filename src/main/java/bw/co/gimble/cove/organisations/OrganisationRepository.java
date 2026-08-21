package bw.co.gimble.cove.organisations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    List<Organisation> findByIdIn(Collection<UUID> ids);

    List<Organisation> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Organisation> findByVerificationStatus(OrgVerificationStatus status);
}