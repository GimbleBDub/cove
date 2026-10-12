package bw.co.gimble.cove.staff;

import bw.co.gimble.cove.auth.RefreshTokenService;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.profile.models.UserProfile;
import bw.co.gimble.cove.profile.repositories.UserProfileRepository;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRole;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.staff.dto.StaffRequests.CreateStaffRequest;
import bw.co.gimble.cove.staff.dto.StaffResponse;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Super admins add Cove staff and decide which parts of the back office each one can use. */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository profileRepository;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        return staffUsers().stream()
                .sorted(Comparator.comparing(User::getCreatedAt))
                .map(u -> map(u, null))
                .toList();
    }

    public StaffResponse create(CreateStaffRequest request) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "That email already has a Cove account. Staff need their own work account.");
        }
        String temporary = TemporaryPasswords.generate();
        User user = userRepository.save(User.builder()
                .email(email)
                .phone(request.phone().trim())
                .passwordHash(passwordEncoder.encode(temporary))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .passwordChangeRequired(true)
                .staffPermissions(new HashSet<>(request.permissions()))
                .build());
        userRoleRepository.save(UserRole.builder()
                .userId(user.getId())
                .role(Role.PLATFORM_ADMIN)
                .status(RoleStatus.ACTIVE)
                .grantedByUserId(actor)
                .grantedAt(Instant.now())
                .build());
        // Staff skip consumer onboarding: a names-only profile satisfies the profile gate.
        profileRepository.save(UserProfile.builder()
                .user(user)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .build());
        auditLogService.record("USER", user.getId(), "STAFF_CREATED", null, request.permissions().toString(), actor, email);
        return map(user, temporary);
    }

    public StaffResponse setPermissions(UUID userId, Set<StaffPermission> permissions) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        User staff = requireStaffUser(userId);
        boolean losingSuper = staff.getStaffPermissions().contains(StaffPermission.SUPER_ADMIN)
                && !permissions.contains(StaffPermission.SUPER_ADMIN);
        if (losingSuper && actor.equals(userId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "You can't remove your own super admin access");
        }
        if (losingSuper) requireAnotherActiveSuperAdmin(userId);
        String before = staff.getStaffPermissions().toString();
        staff.getStaffPermissions().clear();
        staff.getStaffPermissions().addAll(permissions);
        auditLogService.record("USER", userId, "STAFF_PERMISSIONS_CHANGED", before, permissions.toString(), actor, null);
        return map(staff, null);
    }

    /** Forgotten or leaked: sign them out everywhere and issue a new temporary password. */
    public StaffResponse resetPassword(UUID userId) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        if (actor.equals(userId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Use Change password to update your own password");
        }
        User staff = requireStaffUser(userId);
        String temporary = TemporaryPasswords.generate();
        staff.setPasswordHash(passwordEncoder.encode(temporary));
        staff.setPasswordChangeRequired(true);
        signOutEverywhere(staff);
        auditLogService.record("USER", userId, "STAFF_PASSWORD_RESET", null, null, actor, null);
        return map(staff, temporary);
    }

    public StaffResponse setEnabled(UUID userId, boolean enabled, String reason) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        if (actor.equals(userId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "You can't disable your own account");
        }
        User staff = requireStaffUser(userId);
        if (!enabled && staff.getStaffPermissions().contains(StaffPermission.SUPER_ADMIN)) {
            requireAnotherActiveSuperAdmin(userId);
        }
        UserStatus before = staff.getStatus();
        staff.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        if (!enabled) signOutEverywhere(staff);
        auditLogService.record("USER", userId, enabled ? "STAFF_ENABLED" : "STAFF_DISABLED",
                before.name(), staff.getStatus().name(), actor, reason);
        return map(staff, null);
    }

    private void signOutEverywhere(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private void requireAnotherActiveSuperAdmin(UUID exceptUserId) {
        boolean another = staffUsers().stream()
                .anyMatch(u -> !u.getId().equals(exceptUserId)
                        && u.getStatus() == UserStatus.ACTIVE
                        && u.getStaffPermissions().contains(StaffPermission.SUPER_ADMIN));
        if (!another) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Cove needs at least one active super admin");
        }
    }

    private List<User> staffUsers() {
        return userRoleRepository.findAll().stream()
                .filter(r -> r.getRole() == Role.PLATFORM_ADMIN && r.getOrganisationId() == null && r.getStatus() == RoleStatus.ACTIVE)
                .map(UserRole::getUserId)
                .distinct()
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private User requireStaffUser(UUID userId) {
        if (!permissionService.isPlatformAdmin(userId)) {
            throw ApiException.notFound("Staff member not found");
        }
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Staff member not found"));
    }

    private StaffResponse map(User u, String temporaryPassword) {
        UserProfile profile = profileRepository.findByUserId(u.getId()).orElse(null);
        return new StaffResponse(
                u.getId(), u.getEmail(), u.getPhone(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                Set.copyOf(u.getStaffPermissions()),
                u.getStatus() == UserStatus.ACTIVE,
                u.isPasswordChangeRequired(),
                u.getCreatedAt(),
                temporaryPassword);
    }
}
