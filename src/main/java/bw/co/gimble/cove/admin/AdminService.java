package bw.co.gimble.cove.admin;

import bw.co.gimble.cove.admin.AdminDtos.*;
import bw.co.gimble.cove.auth.RefreshTokenService;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogEntry;
import bw.co.gimble.cove.common.AuditLogRepository;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.leads.LeadRepository;
import bw.co.gimble.cove.listings.ListingRepository;
import bw.co.gimble.cove.listings.ListingStatus;
import bw.co.gimble.cove.organisations.OrgVerificationStatus;
import bw.co.gimble.cove.organisations.Organisation;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.profile.enums.KycStatus;
import bw.co.gimble.cove.profile.models.UserKyc;
import bw.co.gimble.cove.profile.repositories.UserKycRepository;
import bw.co.gimble.cove.profile.repositories.UserProfileRepository;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyRepository;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRole;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.staff.StaffPermission;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import bw.co.gimble.cove.viewings.ViewingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-mostly back-office queries. Fine for the current scale (in-memory
 * filtering); move to paged repository queries once tables get large.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private static final int MAX_ROWS = 300;

    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserRoleRepository roleRepository;
    private final UserKycRepository kycRepository;
    private final OrganisationRepository organisationRepository;
    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final LeadRepository leadRepository;
    private final ViewingRepository viewingRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        UUID me = bw.co.gimble.cove.security.AuthSupport.requireUserId();
        if (!permissionService.isPlatformAdmin(me)) {
            throw ApiException.forbidden(ErrorCode.PLATFORM_ADMIN_ONLY, "Cove staff only");
        }
        Instant week = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant month = Instant.now().minus(30, ChronoUnit.DAYS);
        List<User> users = userRepository.findAll();
        return new DashboardResponse(
                users.size(),
                users.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(week)).count(),
                organisationRepository.count(),
                organisationRepository.findByVerificationStatus(OrgVerificationStatus.UNDER_REVIEW).size(),
                kycRepository.findByStatusIn(List.of(KycStatus.PENDING, KycStatus.UNDER_REVIEW)).size(),
                listingRepository.countByStatus(ListingStatus.PENDING_REVIEW),
                listingRepository.countByStatus(ListingStatus.PUBLISHED),
                propertyRepository.count(),
                leadRepository.findAll().stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(month)).count(),
                viewingRepository.findAll().stream().filter(v -> v.getCreatedAt() != null && v.getCreatedAt().isAfter(month)).count()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users(String query) {
        permissionService.requireStaff(true, StaffPermission.SUPPORT, StaffPermission.VERIFIER);
        String q = query == null ? "" : query.trim().toLowerCase();
        Map<UUID, String> names = new HashMap<>();
        profileRepository.findAll().forEach(p -> names.put(p.getUser().getId(), p.getFullName()));
        Map<UUID, List<UserRole>> roles = roleRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoleStatus.ACTIVE)
                .collect(Collectors.groupingBy(UserRole::getUserId));
        Map<UUID, String> orgNames = organisationRepository.findAll().stream()
                .collect(Collectors.toMap(Organisation::getId, Organisation::getName));
        Map<UUID, List<UserKyc>> kyc = kycRepository.findAll().stream()
                .collect(Collectors.groupingBy(k -> k.getUser().getId()));
        return userRepository.findAll().stream()
                .filter(u -> q.isEmpty()
                        || u.getEmail().toLowerCase().contains(q)
                        || (u.getPhone() != null && u.getPhone().contains(q))
                        || names.getOrDefault(u.getId(), "").toLowerCase().contains(q))
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_ROWS)
                .map(u -> {
                    List<UserRole> rs = roles.getOrDefault(u.getId(), List.of());
                    List<UserKyc> docs = kyc.getOrDefault(u.getId(), List.of());
                    return new AdminUserResponse(
                            u.getId(), u.getEmail(), u.getPhone(), names.get(u.getId()),
                            u.getStatus().name(), u.isEmailVerified(),
                            rs.stream().anyMatch(r -> r.getRole() == Role.PLATFORM_ADMIN),
                            rs.stream().map(r -> r.getRole().name()).distinct().toList(),
                            rs.stream().map(UserRole::getOrganisationId).filter(Objects::nonNull).distinct()
                                    .map(orgNames::get).filter(Objects::nonNull).toList(),
                            docs.stream().filter(d -> d.getStatus() == KycStatus.VERIFIED).count(),
                            docs.stream().filter(d -> d.getStatus() == KycStatus.PENDING || d.getStatus() == KycStatus.UNDER_REVIEW).count(),
                            u.getCreatedAt());
                })
                .toList();
    }

    public void setUserSuspended(UUID userId, boolean suspended, String reason) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.SUPPORT);
        if (actor.equals(userId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "You can't suspend yourself");
        }
        if (permissionService.isPlatformAdmin(userId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Staff accounts are managed under Staff");
        }
        if (suspended && (reason == null || reason.isBlank())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Give a reason for suspending this account");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        UserStatus before = user.getStatus();
        user.setStatus(suspended ? UserStatus.SUSPENDED : UserStatus.ACTIVE);
        if (suspended) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            refreshTokenService.revokeAllForUser(userId);
        }
        auditLogService.record("USER", userId, suspended ? "SUSPENDED" : "REACTIVATED",
                before.name(), user.getStatus().name(), actor, reason);
    }

    @Transactional(readOnly = true)
    public List<AdminOrganisationResponse> organisations(String status) {
        permissionService.requireStaff(true, StaffPermission.VERIFIER, StaffPermission.SUPPORT);
        Map<UUID, Long> members = roleRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoleStatus.ACTIVE && r.getOrganisationId() != null)
                .collect(Collectors.groupingBy(UserRole::getOrganisationId,
                        Collectors.mapping(UserRole::getUserId, Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));
        List<Property> properties = propertyRepository.findAll();
        Map<UUID, Long> propertyCounts = properties.stream().filter(p -> p.getManagingOrganisationId() != null)
                .collect(Collectors.groupingBy(Property::getManagingOrganisationId, Collectors.counting()));
        Map<UUID, UUID> propertyOrg = new HashMap<>();
        properties.forEach(p -> { if (p.getManagingOrganisationId() != null) propertyOrg.put(p.getId(), p.getManagingOrganisationId()); });
        Map<UUID, Long> live = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED).stream()
                .map(l -> propertyOrg.get(l.getPropertyId())).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        return organisationRepository.findAll().stream()
                .filter(o -> status == null || status.isBlank() || o.getVerificationStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(Organisation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(o -> new AdminOrganisationResponse(
                        o.getId(), o.getName(), o.getTypes().stream().map(Enum::name).toList(),
                        o.getVerificationStatus().name(), o.getEmail(), o.getPhone(),
                        members.getOrDefault(o.getId(), 0L), propertyCounts.getOrDefault(o.getId(), 0L),
                        live.getOrDefault(o.getId(), 0L), o.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> audit(String entityType, UUID entityId) {
        permissionService.requireStaff(false, StaffPermission.SUPER_ADMIN);
        Map<UUID, String> emails = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, User::getEmail));
        return auditLogRepository.findAll().stream()
                .filter(e -> entityType == null || entityType.isBlank() || e.getEntityType().equalsIgnoreCase(entityType))
                .filter(e -> entityId == null || entityId.equals(e.getEntityId()))
                .sorted(Comparator.comparing(AuditLogEntry::getOccurredAt).reversed())
                .limit(MAX_ROWS)
                .map(e -> new AuditEntryResponse(e.getId(), e.getEntityType(), e.getEntityId(), e.getAction(),
                        e.getFromValue(), e.getToValue(), e.getActorUserId(),
                        e.getActorUserId() == null ? null : emails.get(e.getActorUserId()), e.getNote(), e.getOccurredAt()))
                .toList();
    }
}
