package bw.co.gimble.cove.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
    List<WebhookEndpoint> findByOrganisationIdOrderByCreatedAtDesc(UUID organisationId);
    List<WebhookEndpoint> findByOrganisationIdAndActiveTrue(UUID organisationId);
}
