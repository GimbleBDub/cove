package bw.co.gimble.cove.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserIdAndStatus(UUID userId, RoleStatus status);

    List<UserRole> findByUserIdAndOrganisationIdAndStatus(UUID userId, UUID organisationId, RoleStatus status);

    boolean existsByUserIdAndOrganisationIdAndRoleAndStatus(
            UUID userId, UUID organisationId, Role role, RoleStatus status);

    boolean existsByUserIdAndOrganisationIdAndStatus(UUID userId, UUID organisationId, RoleStatus status);

    boolean existsByUserIdAndRoleAndOrganisationIdIsNullAndStatus(
            UUID userId, Role role, RoleStatus status);

    List<UserRole> findByOrganisationIdAndStatus(UUID organisationId, RoleStatus status);

    List<UserRole> findByOrganisationIdAndRoleAndStatus(UUID organisationId, Role role, RoleStatus status);

    long countByOrganisationIdAndRoleAndStatus(UUID organisationId, Role role, RoleStatus status);

    @Query("select distinct r.organisationId from UserRole r " +
            "where r.userId = :userId and r.status = :status and r.organisationId is not null")
    List<UUID> findDistinctOrganisationIdsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") RoleStatus status);
}