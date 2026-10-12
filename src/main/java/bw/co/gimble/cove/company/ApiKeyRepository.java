package bw.co.gimble.cove.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByPrefix(String prefix);
    List<ApiKey> findByOrganisationIdOrderByCreatedAtDesc(UUID organisationId);
}
