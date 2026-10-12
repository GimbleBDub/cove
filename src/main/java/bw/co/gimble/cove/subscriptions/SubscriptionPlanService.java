package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.subscriptions.dto.PlanResponse;
import bw.co.gimble.cove.subscriptions.dto.UpsertPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final PermissionService permissionService;

    /** Anyone authenticated can see what's available - used for onboarding's subscription step and Settings. */
    @Transactional(readOnly = true)
    public List<PlanResponse> listActive() {
        return planRepository.findByActiveTrue().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> adminListAll() {
        requirePlatformAdmin();
        return planRepository.findAll().stream().map(this::map).toList();
    }

    public PlanResponse adminCreate(UpsertPlanRequest request) {
        requirePlatformAdmin();
        if (planRepository.existsByCode(request.code())) {
            throw ApiException.conflict(ErrorCode.SUBSCRIPTION_PLAN_CODE_TAKEN, "A plan with this code already exists");
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .subjectType(request.subjectType())
                .functions(request.functions())
                .maxEmployees(request.maxEmployees())
                .priceDescription(blankToNull(request.priceDescription()))
                .price(request.price())
                .currency("BWP")
                .billingPeriod(request.billingPeriod())
                .active(true)
                .build();
        return map(planRepository.save(plan));
    }

    public PlanResponse adminUpdate(UUID planId, UpsertPlanRequest request) {
        requirePlatformAdmin();
        SubscriptionPlan plan = getOrThrow(planId);
        if (!plan.getCode().equals(request.code()) && planRepository.existsByCode(request.code())) {
            throw ApiException.conflict(ErrorCode.SUBSCRIPTION_PLAN_CODE_TAKEN, "A plan with this code already exists");
        }
        plan.setCode(request.code());
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setSubjectType(request.subjectType());
        plan.setFunctions(request.functions());
        plan.setMaxEmployees(request.maxEmployees());
        plan.setPriceDescription(blankToNull(request.priceDescription()));
        plan.setPrice(request.price());
        plan.setCurrency("BWP");
        plan.setBillingPeriod(request.billingPeriod());
        return map(plan);
    }

    public PlanResponse adminSetActive(UUID planId, boolean active) {
        requirePlatformAdmin();
        SubscriptionPlan plan = getOrThrow(planId);
        plan.setActive(active);
        return map(plan);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getOrThrow(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND, "Plan not found"));
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

    PlanResponse map(SubscriptionPlan p) {
        return new PlanResponse(
                p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getSubjectType(),
                p.getFunctions(), p.getMaxEmployees(), p.getPriceDescription(), p.isActive(),
                p.getPrice(), p.getCurrency(), p.getBillingPeriod(), priceLabel(p), p.isFree()
        );
    }

    /** "BWP 1,250 / month"; cents only when they're not zero. */
    public static String priceLabel(SubscriptionPlan p) {
        if (p.getPrice() == null) return "Price on request";
        if (p.isFree()) return "Free";
        return formatBwp(p.getPrice()) + " " + (p.getBillingPeriod() == null ? BillingPeriod.MONTHLY : p.getBillingPeriod()).suffix();
    }

    public static String formatBwp(java.math.BigDecimal amount) {
        var nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        boolean whole = amount.stripTrailingZeros().scale() <= 0;
        nf.setMinimumFractionDigits(whole ? 0 : 2);
        nf.setMaximumFractionDigits(whole ? 0 : 2);
        return "BWP " + nf.format(amount);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
