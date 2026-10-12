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
    private final bw.co.gimble.cove.user.UserRepository userRepository;

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

    /** Empty for anyone who is not Cove staff. */
    public java.util.Set<bw.co.gimble.cove.staff.StaffPermission> staffPermissions(UUID userId) {
        if (userId == null || !isPlatformAdmin(userId)) return java.util.Set.of();
        return userRepository.findById(userId)
                .map(u -> java.util.Set.copyOf(u.getStaffPermissions()))
                .orElse(java.util.Set.of());
    }

    /**
     * SUPER_ADMIN passes every check; VIEWER passes read-only checks; otherwise the
     * user needs one of [anyOf].
     */
    public boolean hasStaffPermission(UUID userId, boolean readOnly, bw.co.gimble.cove.staff.StaffPermission... anyOf) {
        var perms = staffPermissions(userId);
        if (perms.isEmpty()) return false;
        if (perms.contains(bw.co.gimble.cove.staff.StaffPermission.SUPER_ADMIN)) return true;
        if (readOnly && perms.contains(bw.co.gimble.cove.staff.StaffPermission.VIEWER)) return true;
        return Arrays.stream(anyOf).anyMatch(perms::contains);
    }

    /** Returns the acting staff member's id, or throws 403. */
    public UUID requireStaff(boolean readOnly, bw.co.gimble.cove.staff.StaffPermission... anyOf) {
        UUID actor = bw.co.gimble.cove.security.AuthSupport.requireUserId();
        if (!hasStaffPermission(actor, readOnly, anyOf)) {
            throw bw.co.gimble.cove.common.ApiException.forbidden(
                    bw.co.gimble.cove.common.ErrorCode.PLATFORM_ADMIN_ONLY, "You don't have access to this part of the back office");
        }
        return actor;
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

    public boolean isAgencyStaff(UUID userId, UUID organisationId) {
        return hasOrgRole(userId, organisationId, Role.AGENT, Role.AGENCY_ADMIN);
    }
}