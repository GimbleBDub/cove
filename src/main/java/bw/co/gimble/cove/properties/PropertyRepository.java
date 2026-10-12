package bw.co.gimble.cove.properties;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByManagingOrganisationIdInOrderByCreatedAtDesc(Collection<UUID> organisationIds);

    @Query("select distinct p from Property p, PropertyOwner o where o.propertyId = p.id and o.userId = :userId order by p.createdAt desc")
    List<Property> findOwnedBy(@Param("userId") UUID userId);

    java.util.List<Property> findByManagingOrganisationId(UUID organisationId);

    java.util.Optional<Property> findByManagingOrganisationIdAndExternalRef(UUID organisationId, String externalRef);
}
