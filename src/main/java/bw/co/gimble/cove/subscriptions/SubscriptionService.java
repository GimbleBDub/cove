package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.organisations.Organisation;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.subscriptions.dto.AssignSubscriptionRequest;
import bw.co.gimble.cove.subscriptions.dto.MyAccessResponse;
import bw.co.gimble.cove.subscriptions.dto.SubscriptionResponse;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService planService;
    private final OrganisationRepository organisationRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    /**
     * Resolves the single effective (non-expired, ACTIVE) subscription for a subject,
     * lazily flipping it to EXPIRED if its expiresAt has passed rather than requiring a
     * separate scheduled sweep to keep reads correct.
     */
    @Transactional(readOnly = true)
    public Subscription findEffective(SubscriptionSubjectType subjectType, UUID subjectId) {
        return subscriptionRepository
                .findFirstBySubjectTypeAndSubjectIdAndStatusOrderByStartsAtDesc(subjectType, subjectId, SubscriptionStatus.ACTIVE)
                .filter(s -> {
                    if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
                        s.setStatus(SubscriptionStatus.EXPIRED);
                        subscriptionRepository.save(s);
                        return false;
                    }
                    return true;
                })
                .orElse(null);
    }

    /** Free organisations are capped at 3 people until a plan raises maxEmployees. */
    @Transactional(readOnly = true)
    public int organisationMemberCap(UUID organisationId) {
        Subscription sub = findEffective(SubscriptionSubjectType.ORGANISATION, organisationId);
        if (sub == null) return 3;
        Integer max = planService.getOrThrow(sub.getPlanId()).getMaxEmployees();
        if (max == null || max <= 0) return Integer.MAX_VALUE;
        return max;
    }
    @Transactional(readOnly = true)
    public boolean hasFunction(SubscriptionSubjectType subjectType, UUID subjectId, SubscriptionFunction function) {
        Subscription sub = findEffective(subjectType, subjectId);
        if (sub == null) return false;
        return planService.getOrThrow(sub.getPlanId()).getFunctions().contains(function);
    }

    @Transactional(readOnly = true)
    public MyAccessResponse myAccess() {
        UUID userId = currentUserId();
        Set<SubscriptionFunction> unlocked = new HashSet<>();

        Subscription individual = findEffective(SubscriptionSubjectType.INDIVIDUAL, userId);
        SubscriptionResponse individualResponse = null;
        if (individual != null) {
            individualResponse = map(individual);
            unlocked.addAll(planService.getOrThrow(individual.getPlanId()).getFunctions());
        }

        List<UUID> orgIds = userRoleRepository.findDistinctOrganisationIdsByUserIdAndStatus(userId, RoleStatus.ACTIVE);
        List<MyAccessResponse.OrgAccess> orgAccessList = new ArrayList<>();
        for (UUID orgId : orgIds) {
            Subscription orgSub = findEffective(SubscriptionSubjectType.ORGANISATION, orgId);
            if (orgSub == null) continue;
            unlocked.addAll(planService.getOrThrow(orgSub.getPlanId()).getFunctions());
            String orgName = organisationRepository.findById(orgId).map(Organisation::getName).orElse(null);
            orgAccessList.add(new MyAccessResponse.OrgAccess(orgId, orgName, map(orgSub)));
        }

        return new MyAccessResponse(individualResponse, orgAccessList, unlocked);
    }

    /** Platform admin: see every subscription across every org and every person. */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> adminListAll() {
        requirePlatformAdmin();
        return subscriptionRepository.findAllByOrderByStartsAtDesc().stream().map(this::map).toList();
    }

    public SubscriptionResponse adminAssign(AssignSubscriptionRequest request) {
        UUID actingUserId = currentUserId();
        requirePlatformAdmin();

        SubscriptionPlan plan = planService.getOrThrow(request.planId());
        if (!plan.isActive()) {
            throw ApiException.conflict(ErrorCode.SUBSCRIPTION_PLAN_INACTIVE, "This plan is deactivated and cannot be assigned");
        }
        if (plan.getSubjectType() != request.subjectType()) {
            throw ApiException.badRequest(ErrorCode.SUBSCRIPTION_SUBJECT_FORBIDDEN,
                    "This plan is for " + plan.getSubjectType() + " subjects, not " + request.subjectType());
        }
        validateSubjectExists(request.subjectType(), request.subjectId());

        Subscription sub = assignInternal(
                request.subjectType(), request.subjectId(), plan.getId(),
                SubscriptionSource.ADMIN_GRANT, request.expiresAt(), actingUserId, null, request.note()
        );
        return map(sub);
    }

    public void adminCancel(UUID subscriptionId) {
        requirePlatformAdmin();
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Subscription not found"));
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) return; // idempotent
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        sub.setCancelledByUserId(currentUserId());
    }

    /**
     * Internal entry point used by both adminAssign and FreeAccessCodeService#redeem.
     * Supersedes any existing ACTIVE subscription for the subject - a subject holds
     * exactly one active plan at a time, keeping "what does this org/person currently
     * see" a single unambiguous lookup.
     */
    public Subscription assignInternal(
            SubscriptionSubjectType subjectType, UUID subjectId, UUID planId,
            SubscriptionSource source, Instant expiresAt, UUID grantedByUserId, UUID freeAccessCodeId, String note
    ) {
        subscriptionRepository
                .findFirstBySubjectTypeAndSubjectIdAndStatusOrderByStartsAtDesc(subjectType, subjectId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    existing.setCancelledAt(Instant.now());
                    existing.setCancelledByUserId(grantedByUserId);
                    subscriptionRepository.save(existing);
                });

        Subscription sub = Subscription.builder()
                .subjectType(subjectType)
                .subjectId(subjectId)
                .planId(planId)
                .status(SubscriptionStatus.ACTIVE)
                .source(source)
                .startsAt(Instant.now())
                .expiresAt(expiresAt)
                .grantedByUserId(grantedByUserId)
                .freeAccessCodeId(freeAccessCodeId)
                .note(note)
                .build();
        return subscriptionRepository.save(sub);
    }

    public void validateSubjectExists(SubscriptionSubjectType subjectType, UUID subjectId) {
        boolean exists = subjectType == SubscriptionSubjectType.ORGANISATION
                ? organisationRepository.existsById(subjectId)
                : userRepository.existsById(subjectId);
        if (!exists) {
            throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND,
                    (subjectType == SubscriptionSubjectType.ORGANISATION ? "Organisation" : "User") + " not found");
        }
    }

    private void requirePlatformAdmin() {
        permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.FINANCE);
    }

    private UUID currentUserId() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthPrincipal principal) {
            return principal.getUserId();
        }
        throw ApiException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Not authenticated");
    }

    private SubscriptionResponse map(Subscription s) {
        String label = s.getSubjectType() == SubscriptionSubjectType.ORGANISATION
                ? organisationRepository.findById(s.getSubjectId()).map(Organisation::getName).orElse(null)
                : userRepository.findById(s.getSubjectId()).map(User::getEmail).orElse(null);
        return new SubscriptionResponse(
                s.getId(), s.getSubjectType(), s.getSubjectId(), label,
                planService.map(planService.getOrThrow(s.getPlanId())),
                s.getStatus(), s.getSource(), s.getStartsAt(), s.getExpiresAt(),
                s.getGrantedByUserId(), s.getNote()
        );
    }
}
