package bw.co.gimble.cove.organisations;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.OrganisationCreatedEvent;
import bw.co.gimble.cove.events.OrganisationJoinRequestedEvent;
import bw.co.gimble.cove.events.OrganisationJoinReviewedEvent;
import bw.co.gimble.cove.organisations.dto.*;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.role.UserRoleService;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.storage.StorageService;
import bw.co.gimble.cove.storage.dto.FileValidation;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Individuals never need this - they self-assign personal-capacity roles (OWNER/TENANT/BUYER)
 * directly via UserRoleService. This module only exists for the "acting on behalf of an
 * organisation" path: create one (you become its first AGENCY_ADMIN) or ask to join one that
 * already exists (an existing admin must approve, which is what actually grants the role).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationService {

    private final bw.co.gimble.cove.user.UserRepository joinRequestUsers;
    private final bw.co.gimble.cove.profile.repositories.UserProfileRepository joinRequestProfiles;
    private final OrganisationRepository organisationRepository;
    private final OrganisationJoinRequestRepository joinRequestRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;

    // ─── CREATE / READ ──────────────────────────────────────────

    public OrganisationResponse create(CreateOrganisationRequest request) {
        UUID userId = currentUserId();

        Organisation organisation = Organisation.builder()
                .name(request.name())
                .slug(uniqueSlug(request.name()))
                .types(request.types())
                .description(request.description())
                .email(request.email())
                .phone(request.phone())
                .website(request.website())
                .build();
        organisation = organisationRepository.save(organisation);

        // The creator is the org's first admin - granted directly, not through the
        // normal admin-approval path, since no admin exists yet to approve it.
        userRoleService.grantInitialOrgAdmin(organisation.getId(), userId, organisation.getName());

        eventPublisher.publishEvent(new OrganisationCreatedEvent(organisation.getId(), organisation.getName(), userId));

        return map(organisation);
    }

    public OrganisationResponse update(UUID organisationId, UpdateOrganisationRequest request) {
        UUID userId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);

        if (!permissionService.canManageOrg(userId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can edit it");
        }

        organisation.setName(request.name());
        organisation.setTypes(request.types());
        organisation.setDescription(request.description());
        organisation.setEmail(request.email());
        organisation.setPhone(request.phone());
        organisation.setWebsite(request.website());
        organisation.setDiscoverable(request.discoverable());

        return map(organisation);
    }

    @Transactional(readOnly = true)
    public OrganisationResponse get(UUID organisationId) {
        return map(getOrThrow(organisationId));
    }

    /** Admin-only. Replaces any existing logo. */
    public OrganisationResponse updateLogo(UUID organisationId, MultipartFile file) {
        UUID userId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);

        if (!permissionService.canManageOrg(userId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can change its logo");
        }

        FileValidation.requireImage(file);
        UploadedFile uploaded = storageService.uploadOrganisationLogo(organisationId, file);
        organisation.setLogoUrl(uploaded.publicUrl());

        return map(organisation);
    }

    /** Simple name search over discoverable organisations - what a user sees before deciding to join one. */
    @Transactional(readOnly = true)
    public List<OrganisationSummaryResponse> search(String query) {
        List<Organisation> results = (query == null || query.isBlank())
                ? organisationRepository.findAll()
                : organisationRepository.findByNameContainingIgnoreCase(query.trim());

        return results.stream()
                .filter(Organisation::isDiscoverable)
                .map(this::mapSummary)
                .toList();
    }

    /** Organisations the current user holds any active role at. */
    @Transactional(readOnly = true)
    public List<OrganisationResponse> myOrganisations() {
        List<UUID> orgIds = userRoleRepository.findDistinctOrganisationIdsByUserIdAndStatus(currentUserId(), RoleStatus.ACTIVE);
        return organisationRepository.findByIdIn(orgIds).stream().map(this::map).toList();
    }

    // ─── JOIN REQUESTS ──────────────────────────────────────────

    public JoinRequestResponse requestToJoin(UUID organisationId, RequestJoinOrganisationRequest request) {
        UUID userId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);

        if (permissionService.isOrgMember(userId, organisationId)) {
            throw ApiException.conflict(ErrorCode.ALREADY_ORGANISATION_MEMBER, "You are already a member of this organisation");
        }
        if (joinRequestRepository.existsByOrganisationIdAndUserIdAndStatus(organisationId, userId, JoinRequestStatus.PENDING)) {
            throw ApiException.conflict(ErrorCode.JOIN_REQUEST_ALREADY_PENDING, "You already have a pending request to join this organisation");
        }

        OrganisationJoinRequest joinRequest = OrganisationJoinRequest.builder()
                .organisationId(organisationId)
                .userId(userId)
                .requestedRole(request.requestedRole())
                .message(request.message())
                .status(JoinRequestStatus.PENDING)
                .build();
        joinRequest = joinRequestRepository.save(joinRequest);

        eventPublisher.publishEvent(new OrganisationJoinRequestedEvent(
                joinRequest.getId(), organisationId, organisation.getName(), userId));

        return map(joinRequest, organisation.getName());
    }

    public void cancelJoinRequest(UUID organisationId, UUID requestId) {
        UUID userId = currentUserId();
        OrganisationJoinRequest joinRequest = joinRequestRepository.findByIdAndOrganisationId(requestId, organisationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.JOIN_REQUEST_NOT_FOUND, "Join request not found"));

        if (!permissionService.isSelf(userId, joinRequest.getUserId())) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You can only cancel your own join request");
        }
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw ApiException.conflict(ErrorCode.JOIN_REQUEST_ALREADY_REVIEWED, "This request has already been reviewed");
        }
        joinRequest.setStatus(JoinRequestStatus.CANCELLED);
    }

    /** Admin view: pending (or all, if status omitted) requests for their organisation. */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listJoinRequests(UUID organisationId, JoinRequestStatus status) {
        UUID userId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);

        if (!permissionService.canManageOrg(userId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can view join requests");
        }

        JoinRequestStatus effectiveStatus = status != null ? status : JoinRequestStatus.PENDING;
        return joinRequestRepository.findByOrganisationIdAndStatus(organisationId, effectiveStatus)
                .stream().map(r -> map(r, organisation.getName())).toList();
    }

    /** The current user's own join requests, across all organisations. */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> myJoinRequests() {
        UUID userId = currentUserId();
        List<OrganisationJoinRequest> requests = joinRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return requests.stream().map(r -> map(r, orgName(r.getOrganisationId()))).toList();
    }

    /** Approving grants the role via UserRoleService, which is the single place UserRole rows are written. */
    public JoinRequestResponse approve(UUID organisationId, UUID requestId, Role grantedRole) {
        UUID adminId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);
        OrganisationJoinRequest joinRequest = requirePending(organisationId, requestId);

        if (!permissionService.canManageOrg(adminId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can review join requests");
        }

        Role roleToGrant = grantedRole != null ? grantedRole : joinRequest.getRequestedRole();
        if (roleToGrant == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "A role must be specified to approve this request");
        }

        userRoleService.assignOrgRole(organisationId, joinRequest.getUserId(), roleToGrant);

        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setReviewedByUserId(adminId);
        joinRequest.setReviewedAt(Instant.now());

        eventPublisher.publishEvent(new OrganisationJoinReviewedEvent(
                joinRequest.getId(), organisationId, organisation.getName(), joinRequest.getUserId(), true, null));

        return map(joinRequest, organisation.getName());
    }

    public JoinRequestResponse reject(UUID organisationId, UUID requestId, String reason) {
        UUID adminId = currentUserId();
        Organisation organisation = getOrThrow(organisationId);
        OrganisationJoinRequest joinRequest = requirePending(organisationId, requestId);

        if (!permissionService.canManageOrg(adminId, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an admin of this organisation can review join requests");
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setReviewedByUserId(adminId);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setRejectionReason(reason);

        eventPublisher.publishEvent(new OrganisationJoinReviewedEvent(
                joinRequest.getId(), organisationId, organisation.getName(), joinRequest.getUserId(), false, reason));

        return map(joinRequest, organisation.getName());
    }

    // ─── HELPERS ────────────────────────────────────────────────

    private OrganisationJoinRequest requirePending(UUID organisationId, UUID requestId) {
        OrganisationJoinRequest joinRequest = joinRequestRepository.findByIdAndOrganisationId(requestId, organisationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.JOIN_REQUEST_NOT_FOUND, "Join request not found"));
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw ApiException.conflict(ErrorCode.JOIN_REQUEST_ALREADY_REVIEWED, "This request has already been reviewed");
        }
        return joinRequest;
    }

    private Organisation getOrThrow(UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.ORGANISATION_NOT_FOUND, "Organisation not found"));
    }

    private String orgName(UUID organisationId) {
        return organisationRepository.findById(organisationId).map(Organisation::getName).orElse(null);
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "org";
        }
        String candidate = base;
        int suffix = 1;
        while (organisationRepository.existsByNameIgnoreCase(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    private UUID currentUserId() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthPrincipal principal) {
            return principal.getUserId();
        }
        throw ApiException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Not authenticated");
    }

    private OrganisationResponse map(Organisation o) {
        return new OrganisationResponse(
                o.getId(), o.getName(), o.getSlug(), o.getTypes(), o.getVerificationStatus(),
                o.getDescription(), o.getEmail(), o.getPhone(), o.getWebsite(), o.getLogoUrl(),
                o.isDiscoverable(), o.getCreatedAt()
        );
    }

    private OrganisationSummaryResponse mapSummary(Organisation o) {
        return new OrganisationSummaryResponse(o.getId(), o.getName(), o.getSlug(), o.getTypes(), o.getVerificationStatus(), o.getLogoUrl());
    }

    private JoinRequestResponse map(OrganisationJoinRequest r, String organisationName) {
        return new JoinRequestResponse(
                r.getId(), r.getOrganisationId(), organisationName, r.getUserId(), r.getRequestedRole(),
                r.getMessage(), r.getStatus(), r.getReviewedByUserId(), r.getReviewedAt(), r.getRejectionReason(), r.getCreatedAt(),
                joinRequestUsers.findById(r.getUserId()).map(bw.co.gimble.cove.user.User::getEmail).orElse(null),
                joinRequestProfiles.findByUserId(r.getUserId()).map(bw.co.gimble.cove.profile.models.UserProfile::getFullName).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<OrganisationSummaryResponse> listPendingVerification() {
        requirePlatformAdmin();
        return organisationRepository.findByVerificationStatus(OrgVerificationStatus.UNDER_REVIEW)
                .stream().map(this::mapSummary).toList();
    }

    public OrganisationResponse verify(UUID organisationId) {
        requirePlatformAdmin();
        Organisation organisation = getOrThrow(organisationId);
        if (organisation.getVerificationStatus() == OrgVerificationStatus.VERIFIED) {
            throw ApiException.conflict(ErrorCode.ORGANISATION_ALREADY_DECIDED, "This organisation is already verified");
        }
        organisation.setVerificationStatus(OrgVerificationStatus.VERIFIED);
        return map(organisation);
    }

    public OrganisationResponse rejectVerification(UUID organisationId) {
        requirePlatformAdmin();
        Organisation organisation = getOrThrow(organisationId);
        organisation.setVerificationStatus(OrgVerificationStatus.REJECTED);
        return map(organisation);
    }

    private void requirePlatformAdmin() {
        permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.VERIFIER);
    }
}