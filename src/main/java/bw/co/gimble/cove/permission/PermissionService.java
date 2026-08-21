package bw.co.gimble.cove.permission;


import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleRepository userRoleRepository;

    public boolean hasOrgRole(UUID userId, UUID organisationId, Role... anyOf) {
        if (organisationId == null) return false;
        return Arrays.stream(anyOf).anyMatch(role ->
                userRoleRepository.existsByUserIdAndOrganisationIdAndRoleAndStatus(
                        userId, organisationId, role, RoleStatus.ACTIVE));
    }

    public boolean hasGlobalRole(UUID userId, Role role) {
        return userRoleRepository.existsByUserIdAndRoleAndOrganisationIdIsNullAndStatus(
                userId, role, RoleStatus.ACTIVE);
    }

    public boolean isPlatformAdmin(UUID userId) {
        return hasGlobalRole(userId, Role.PLATFORM_ADMIN);
    }

    public boolean canManageOrg(UUID userId, UUID organisationId) {
        return isPlatformAdmin(userId) || hasOrgRole(userId, organisationId, Role.AGENCY_ADMIN);
    }

    public boolean isOrgMember(UUID userId, UUID organisationId) {
        if (organisationId == null) return false;
        return userRoleRepository.existsByUserIdAndOrganisationIdAndStatus(userId, organisationId, RoleStatus.ACTIVE);
    }

    public boolean isSelf(UUID userId, UUID targetUserId) {
        return userId != null && userId.equals(targetUserId);
    }
}