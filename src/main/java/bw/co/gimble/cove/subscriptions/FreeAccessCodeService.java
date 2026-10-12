package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.subscriptions.dto.AccessCodeResponse;
import bw.co.gimble.cove.subscriptions.dto.CreateAccessCodeRequest;
import bw.co.gimble.cove.subscriptions.dto.RedeemCodeRequest;
import bw.co.gimble.cove.subscriptions.dto.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FreeAccessCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I - avoids transcription errors
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FreeAccessCodeRepository codeRepository;
    private final CodeRedemptionRepository redemptionRepository;
    private final SubscriptionPlanService planService;
    private final SubscriptionService subscriptionService;
    private final PermissionService permissionService;

    public AccessCodeResponse adminCreate(CreateAccessCodeRequest request) {
        requirePlatformAdmin();
        SubscriptionPlan plan = planService.getOrThrow(request.planId());

        String code = (request.code() == null || request.code().isBlank())
                ? generateCode()
                : request.code().trim().toUpperCase();
        if (codeRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw ApiException.conflict(ErrorCode.SUBSCRIPTION_PLAN_CODE_TAKEN, "This code is already in use");
        }

        FreeAccessCode entity = FreeAccessCode.builder()
                .code(code)
                .planId(plan.getId())
                .durationDays(request.durationDays())
                .maxRedemptions(request.maxRedemptions())
                .redemptionCount(0)
                .expiresAt(request.expiresAt())
                .active(true)
                .note(request.note())
                .build();
        return map(codeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<AccessCodeResponse> adminList() {
        requirePlatformAdmin();
        return codeRepository.findAllByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }

    public AccessCodeResponse adminSetActive(UUID codeId, boolean active) {
        requirePlatformAdmin();
        FreeAccessCode code = getOrThrow(codeId);
        code.setActive(active);
        return map(code);
    }

    /**
     * Self-service redemption. For an ORGANISATION subject, the caller must be able to
     * manage that org (agency admin or platform admin) - a regular member can't attach a
     * paid/free plan to their whole organisation on a whim.
     */
    public SubscriptionResponse redeem(RedeemCodeRequest request) {
        UUID userId = currentUserId();
        FreeAccessCode code = codeRepository.findByCodeIgnoreCase(request.code().trim())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.ACCESS_CODE_NOT_FOUND, "That code isn't valid"));

        if (!code.isActive()) {
            throw ApiException.conflict(ErrorCode.ACCESS_CODE_INACTIVE, "This code is no longer active");
        }
        if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict(ErrorCode.ACCESS_CODE_EXPIRED, "This code has expired");
        }
        if (code.getMaxRedemptions() != null && code.getRedemptionCount() >= code.getMaxRedemptions()) {
            throw ApiException.conflict(ErrorCode.ACCESS_CODE_LIMIT_REACHED, "This code has already been fully redeemed");
        }

        SubscriptionPlan plan = planService.getOrThrow(code.getPlanId());
        if (plan.getSubjectType() != request.subjectType()) {
            throw ApiException.badRequest(ErrorCode.SUBSCRIPTION_SUBJECT_FORBIDDEN,
                    "This code grants a " + plan.getSubjectType() + " plan and can't be applied to a " + request.subjectType() + " account");
        }

        UUID subjectId = request.subjectType() == SubscriptionSubjectType.INDIVIDUAL
                ? userId
                : requireOrgId(request.subjectId());

        if (request.subjectType() == SubscriptionSubjectType.ORGANISATION
                && !permissionService.canManageOrg(userId, subjectId)) {
            throw ApiException.forbidden(ErrorCode.SUBSCRIPTION_SUBJECT_FORBIDDEN,
                    "Only an admin of this organisation can redeem a code for it");
        }

        subscriptionService.validateSubjectExists(request.subjectType(), subjectId);

        Instant expiresAt = code.getDurationDays() != null
                ? Instant.now().plus(code.getDurationDays(), ChronoUnit.DAYS)
                : null;

        Subscription sub = subscriptionService.assignInternal(
                request.subjectType(), subjectId, plan.getId(),
                SubscriptionSource.FREE_CODE, expiresAt, userId, code.getId(),
                "Redeemed via code " + code.getCode()
        );

        code.setRedemptionCount(code.getRedemptionCount() + 1);

        redemptionRepository.save(CodeRedemption.builder()
                .codeId(code.getId())
                .redeemedByUserId(userId)
                .subjectType(request.subjectType())
                .subjectId(subjectId)
                .subscriptionId(sub.getId())
                .build());

        return new SubscriptionResponse(
                sub.getId(), sub.getSubjectType(), sub.getSubjectId(), null,
                planService.map(plan), sub.getStatus(), sub.getSource(),
                sub.getStartsAt(), sub.getExpiresAt(), sub.getGrantedByUserId(), sub.getNote()
        );
    }

    private UUID requireOrgId(UUID subjectId) {
        if (subjectId == null) {
            throw ApiException.badRequest(ErrorCode.SUBSCRIPTION_SUBJECT_FORBIDDEN, "subjectId is required for an organisation redemption");
        }
        return subjectId;
    }

    private FreeAccessCode getOrThrow(UUID codeId) {
        return codeRepository.findById(codeId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.ACCESS_CODE_NOT_FOUND, "Code not found"));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("COVE-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
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

    private AccessCodeResponse map(FreeAccessCode c) {
        return new AccessCodeResponse(
                c.getId(), c.getCode(), planService.map(planService.getOrThrow(c.getPlanId())),
                c.getDurationDays(), c.getMaxRedemptions(), c.getRedemptionCount(),
                c.getExpiresAt(), c.isActive(), c.getNote()
        );
    }
}
