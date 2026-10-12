package bw.co.gimble.cove.role;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.RoleAssignedEvent;
import bw.co.gimble.cove.events.RoleRevokedEvent;
import bw.co.gimble.cove.organisations.Organisation;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.role.dto.RoleResponse;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.subscriptions.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final OrganisationRepository organisationRepository;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public List<RoleResponse> myRoles() {
        return mapAll(userRoleRepository.findByUserIdAndStatus(currentUserId(), RoleStatus.ACTIVE));
    }

    public RoleResponse assignPersonalRole(Role role) {
        UUID userId = currentUserId();
        assertAssignable(role);
        if (role.requiresOrganisation()) {
            throw ApiException.badRequest(ErrorCode.ROLE_DOES_NOT_REQUIRE_ORGANISATION,
                    role + " requires an organisation and cannot be self-assigned this way");
        }
        if (userRoleRepository.existsByUserIdAndRoleAndOrganisationIdIsNullAndStatus(userId, role, RoleStatus.ACTIVE)) {
            throw ApiException.conflict(ErrorCode.ROLE_ALREADY_ASSIGNED, "You already hold the " + role + " role");
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .role(role)
                .organisationId(null)
                .status(RoleStatus.ACTIVE)
                .grantedByUserId(userId)
                .grantedAt(Instant.now())
                .build();
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publishEvent(new RoleAssignedEvent(
                userRole.getId(), userId, role, null, null, userId));

        return map(userRole, null);
    }

    public RoleResponse assignOrgRole(UUID organisationId, UUID targetUserId, Role role) {
        UUID actingUserId = currentUserId();
        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> ApiException.notFound("Organisation not found"));

        assertAssignable(role);
        if (!role.requiresOrganisation()) {
            throw ApiException.badRequest(ErrorCode.ROLE_NOT_APPLICABLE_TO_ORGANISATION,
                    role + " is a personal-capacity role and cannot be granted at an organisation");
        }
        if (!permissionService.canManageOrg(actingUserId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can assign roles");
        }
        if (userRoleRepository.existsByUserIdAndOrganisationIdAndRoleAndStatus(targetUserId, organisationId, role, RoleStatus.ACTIVE)) {
            throw ApiException.conflict(ErrorCode.ROLE_ALREADY_ASSIGNED, "User already holds this role at this organisation");
        }
        enforceMemberCap(organisationId, targetUserId);

        UserRole userRole = UserRole.builder()
                .userId(targetUserId)
                .role(role)
                .organisationId(organisationId)
                .status(RoleStatus.ACTIVE)
                .grantedByUserId(actingUserId)
                .grantedAt(Instant.now())
                .build();
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publishEvent(new RoleAssignedEvent(
                userRole.getId(), targetUserId, role, organisationId, org.getName(), actingUserId));

        return map(userRole, org.getName());
    }

    public RoleResponse grantInitialOrgAdmin(UUID organisationId, UUID userId, String organisationName) {
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .role(Role.AGENCY_ADMIN)
                .organisationId(organisationId)
                .status(RoleStatus.ACTIVE)
                .grantedByUserId(userId)
                .grantedAt(Instant.now())
                .build();
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publishEvent(new RoleAssignedEvent(
                userRole.getId(), userId, Role.AGENCY_ADMIN, organisationId, organisationName, userId));

        return map(userRole, organisationName);
    }

    public void revokeRole(UUID roleId) {
        UUID actingUserId = currentUserId();
        UserRole userRole = userRoleRepository.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("Role grant not found"));

        String organisationName = null;
        if (userRole.getOrganisationId() != null) {
            if (!permissionService.canManageOrg(actingUserId, userRole.getOrganisationId())) {
                throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can revoke roles");
            }
            if (userRole.getRole() == Role.AGENCY_ADMIN
                    && userRoleRepository.countByOrganisationIdAndRoleAndStatus(
                    userRole.getOrganisationId(), Role.AGENCY_ADMIN, RoleStatus.ACTIVE) <= 1) {
                throw ApiException.conflict(ErrorCode.CANNOT_REVOKE_LAST_ORG_ADMIN,
                        "This organisation must always have at least one admin");
            }
            organisationName = organisationRepository.findById(userRole.getOrganisationId())
                    .map(Organisation::getName).orElse(null);
        } else if (!permissionService.isSelf(actingUserId, userRole.getUserId()) && !permissionService.isPlatformAdmin(actingUserId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You cannot revoke another user's personal role");
        }

        if (userRole.getStatus() != RoleStatus.ACTIVE) {
            return; // already revoked - idempotent
        }

        userRole.setStatus(RoleStatus.REVOKED);
        userRole.setRevokedAt(Instant.now());
        userRole.setRevokedByUserId(actingUserId);

        eventPublisher.publishEvent(new RoleRevokedEvent(
                userRole.getId(), userRole.getUserId(), userRole.getRole(),
                userRole.getOrganisationId(), organisationName, actingUserId));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listOrgMembers(UUID organisationId) {
        UUID actingUserId = currentUserId();
        if (!permissionService.isOrgMember(actingUserId, organisationId) && !permissionService.isPlatformAdmin(actingUserId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You are not a member of this organisation");
        }
        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> ApiException.notFound("Organisation not found"));
        return mapAll(userRoleRepository.findByOrganisationIdAndStatus(organisationId, RoleStatus.ACTIVE), org.getName());
    }

    /**
     * The guarded path PLATFORM_ADMIN was carved out for in Role's javadoc - only an
     * existing platform admin can mint another one. There is no bootstrap endpoint on
     * purpose; the very first platform admin has to be seeded directly in the database.
     */
    public RoleResponse grantPlatformAdmin(UUID targetUserId) {
        UUID actingUserId = currentUserId();
        permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.SUPER_ADMIN);
        if (userRoleRepository.existsByUserIdAndRoleAndOrganisationIdIsNullAndStatus(targetUserId, Role.PLATFORM_ADMIN, RoleStatus.ACTIVE)) {
            throw ApiException.conflict(ErrorCode.ROLE_ALREADY_ASSIGNED, "This user is already a platform admin");
        }

        UserRole userRole = UserRole.builder()
                .userId(targetUserId)
                .role(Role.PLATFORM_ADMIN)
                .organisationId(null)
                .status(RoleStatus.ACTIVE)
                .grantedByUserId(actingUserId)
                .grantedAt(Instant.now())
                .build();
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publishEvent(new RoleAssignedEvent(
                userRole.getId(), targetUserId, Role.PLATFORM_ADMIN, null, null, actingUserId));

        return map(userRole, null);
    }

    public void revokePlatformAdmin(UUID targetUserId) {
        UUID actingUserId = currentUserId();
        permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.SUPER_ADMIN);
        UserRole userRole = userRoleRepository
                .findByUserIdAndRoleAndOrganisationIdIsNullAndStatus(targetUserId, Role.PLATFORM_ADMIN, RoleStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("This user is not a platform admin"));

        if (userRoleRepository.countByRoleAndOrganisationIdIsNullAndStatus(Role.PLATFORM_ADMIN, RoleStatus.ACTIVE) <= 1) {
            throw ApiException.conflict(ErrorCode.CANNOT_REVOKE_LAST_PLATFORM_ADMIN, "There must always be at least one platform admin");
        }

        userRole.setStatus(RoleStatus.REVOKED);
        userRole.setRevokedAt(Instant.now());
        userRole.setRevokedByUserId(actingUserId);

        eventPublisher.publishEvent(new RoleRevokedEvent(
                userRole.getId(), targetUserId, Role.PLATFORM_ADMIN, null, null, actingUserId));
    }

    private void assertAssignable(Role role) {
        if (role == Role.PLATFORM_ADMIN) {
            throw ApiException.forbidden(ErrorCode.CANNOT_ASSIGN_PLATFORM_ADMIN,
                    "PLATFORM_ADMIN cannot be granted through this endpoint");
        }
    }

    private void enforceMemberCap(UUID organisationId, UUID targetUserId) {
        if (userRoleRepository.existsByUserIdAndOrganisationIdAndStatus(targetUserId, organisationId, RoleStatus.ACTIVE)) {
            return;
        }
        long members = userRoleRepository.countDistinctUsersByOrganisationIdAndStatus(organisationId, RoleStatus.ACTIVE);
        int cap = subscriptionService.organisationMemberCap(organisationId);
        if (members >= cap) {
            throw ApiException.conflict(ErrorCode.ORG_MEMBER_LIMIT,
                    "This organisation can have at most " + cap + " people on its current plan. Subscribe to add more.");
        }
    }

    private UUID currentUserId() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthPrincipal principal) {
            return principal.getUserId();
        }
        throw ApiException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Not authenticated");
    }

    private List<RoleResponse> mapAll(List<UserRole> roles) {
        Map<UUID, String> orgNames = organisationRepository
                .findByIdIn(roles.stream().map(UserRole::getOrganisationId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Organisation::getId, Organisation::getName));
        return roles.stream().map(r -> map(r, orgNames.get(r.getOrganisationId()))).toList();
    }

    private List<RoleResponse> mapAll(List<UserRole> roles, String organisationName) {
        return roles.stream().map(r -> map(r, organisationName)).toList();
    }

    private RoleResponse map(UserRole r, String organisationName) {
        return new RoleResponse(
                r.getId(), r.getUserId(), r.getRole(), r.getOrganisationId(), organisationName,
                r.getStatus(), r.getGrantedByUserId(), r.getGrantedAt(), r.getRevokedAt()
        );
    }
}