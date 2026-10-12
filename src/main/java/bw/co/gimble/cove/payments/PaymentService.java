package bw.co.gimble.cove.payments;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.payments.PaymentDtos.*;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.staff.StaffPermission;
import bw.co.gimble.cove.subscriptions.*;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Plan checkout, DPO-style: checkout() creates the transaction and gateway
 * token, the payer completes it on the gateway page, and verify() reads the
 * outcome. Paid transactions activate the plan for one billing period.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentTransactionRepository repository;
    private final PaymentGateway gateway;
    private final SubscriptionPlanService planService;
    private final SubscriptionService subscriptionService;
    private final PermissionService permissionService;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;

    public PaymentResponse checkout(CheckoutRequest request) {
        UUID payer = AuthSupport.requireUserId();
        if (permissionService.isPlatformAdmin(payer)) {
            throw ApiException.forbidden(ErrorCode.PLATFORM_ADMIN_ONLY, "Cove staff accounts don't subscribe to plans");
        }
        SubscriptionPlan plan = planService.getOrThrow(request.planId());
        if (!plan.isActive()) {
            throw ApiException.conflict(ErrorCode.SUBSCRIPTION_PLAN_INACTIVE, "This plan is no longer available");
        }
        if (plan.getPrice() == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "This plan has no price yet. Contact Cove to subscribe.");
        }
        if (plan.getSubjectType() != request.subjectType()) {
            throw ApiException.badRequest(ErrorCode.SUBSCRIPTION_SUBJECT_FORBIDDEN,
                    plan.getSubjectType() == SubscriptionSubjectType.ORGANISATION
                            ? "This plan is for companies" : "This plan is for individuals");
        }
        UUID subjectId = request.subjectType() == SubscriptionSubjectType.INDIVIDUAL ? payer : request.subjectId();
        if (request.subjectType() == SubscriptionSubjectType.ORGANISATION) {
            if (subjectId == null || !permissionService.canManageOrg(payer, subjectId)) {
                throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only a company admin can pay for the company's plan");
            }
        }
        subscriptionService.validateSubjectExists(request.subjectType(), subjectId);

        BillingPeriod period = plan.getBillingPeriod() == null ? BillingPeriod.MONTHLY : plan.getBillingPeriod();
        PaymentTransaction tx = PaymentTransaction.builder()
                .companyRef(newCompanyRef())
                .payerUserId(payer)
                .subjectType(request.subjectType())
                .subjectId(subjectId)
                .planId(plan.getId())
                .amount(plan.getPrice())
                .currency("BWP")
                .description(plan.getName() + " (" + (period == BillingPeriod.MONTHLY ? "1 month" : "1 year") + ")")
                .status(PaymentStatus.PENDING)
                .expiresAt(Instant.now().plus(TOKEN_LIFETIME))
                .gateway(gateway.name())
                .transToken("pending")
                .build();

        if (plan.isFree()) {
            // Nothing to charge: activate straight away, but keep a zero-amount record.
            tx.setTransToken("FREE-" + tx.getCompanyRef());
            tx = repository.save(tx);
            markPaid(tx, null, "FREE");
            return map(tx, null);
        }

        PaymentGateway.CreatedToken token = gateway.createToken(tx);
        tx.setTransToken(token.transToken());
        tx = repository.save(tx);
        auditLogService.record("PAYMENT", tx.getId(), "CREATED", null, PaymentStatus.PENDING.name(), payer,
                tx.getCompanyRef() + " " + SubscriptionPlanService.formatBwp(tx.getAmount()));
        return map(tx, token.paymentUrl());
    }

    @Transactional
    public PaymentResponse get(String transToken) {
        PaymentTransaction tx = requireMine(transToken);
        expireIfStale(tx);
        return map(tx, null);
    }

    /** Demo gateway only: the tester decides the outcome on the checkout page. */
    public PaymentResponse demoComplete(String transToken, DemoCompleteRequest request) {
        if (!"demo".equals(gateway.name())) {
            throw ApiException.forbidden(ErrorCode.VALIDATION_FAILED, "Demo payments are disabled");
        }
        PaymentTransaction tx = requireMine(transToken);
        expireIfStale(tx);
        if (tx.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "This payment is already " + tx.getStatus().name().toLowerCase());
        }
        tx.setMethod(request.method());
        if (request.approve()) {
            markPaid(tx, request.method(), "DEMO-" + String.format("%08d", RANDOM.nextInt(100_000_000)));
        } else {
            tx.setStatus(PaymentStatus.DECLINED);
            tx.setCompletedAt(Instant.now());
            tx.setFailureReason(request.method() == PaymentMethod.CARD
                    ? "Card declined by issuer (demo)"
                    : "Payment not approved on the phone (demo)");
            auditLogService.record("PAYMENT", tx.getId(), "DECLINED", PaymentStatus.PENDING.name(), PaymentStatus.DECLINED.name(),
                    tx.getPayerUserId(), tx.getCompanyRef());
        }
        return map(tx, null);
    }

    public PaymentResponse cancel(String transToken) {
        PaymentTransaction tx = requireMine(transToken);
        if (tx.getStatus() == PaymentStatus.PENDING) {
            tx.setStatus(PaymentStatus.CANCELLED);
            tx.setCompletedAt(Instant.now());
        }
        return map(tx, null);
    }

    /** Mirrors DPO verifyToken. */
    public VerifyResponse verify(String transToken) {
        PaymentTransaction tx = requireMine(transToken);
        expireIfStale(tx);
        PaymentGateway.Verification v = gateway.verifyToken(tx);
        return new VerifyResponse(tx.getTransToken(), v.resultCode(), v.explanation(), tx.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> mine() {
        UUID payer = AuthSupport.requireUserId();
        return repository.findByPayerUserIdOrderByCreatedAtDesc(payer).stream().map(t -> map(t, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> all() {
        permissionService.requireStaff(true, StaffPermission.FINANCE);
        return repository.findAllByOrderByCreatedAtDesc().stream().limit(500).map(t -> map(t, null)).toList();
    }

    private void markPaid(PaymentTransaction tx, PaymentMethod method, String approvalRef) {
        SubscriptionPlan plan = planService.getOrThrow(tx.getPlanId());
        BillingPeriod period = plan.getBillingPeriod() == null ? BillingPeriod.MONTHLY : plan.getBillingPeriod();
        // Paying again while a paid period is still running extends it rather than losing days.
        Instant start = Instant.now();
        Subscription current = subscriptionService.findEffective(tx.getSubjectType(), tx.getSubjectId());
        if (current != null && current.getPlanId().equals(plan.getId()) && current.getExpiresAt() != null
                && current.getExpiresAt().isAfter(start)) {
            start = current.getExpiresAt();
        }
        Subscription sub = subscriptionService.assignInternal(tx.getSubjectType(), tx.getSubjectId(), plan.getId(),
                SubscriptionSource.PAID, period.endFrom(start), tx.getPayerUserId(), null,
                "Payment " + tx.getCompanyRef());
        tx.setStatus(PaymentStatus.PAID);
        tx.setMethod(method);
        tx.setApprovalRef(approvalRef);
        tx.setCompletedAt(Instant.now());
        tx.setSubscriptionId(sub.getId());
        auditLogService.record("PAYMENT", tx.getId(), "PAID", PaymentStatus.PENDING.name(), PaymentStatus.PAID.name(),
                tx.getPayerUserId(), tx.getCompanyRef() + " " + SubscriptionPlanService.formatBwp(tx.getAmount()));
    }

    private void expireIfStale(PaymentTransaction tx) {
        if (tx.getStatus() == PaymentStatus.PENDING && tx.getExpiresAt().isBefore(Instant.now())) {
            tx.setStatus(PaymentStatus.EXPIRED);
            tx.setCompletedAt(Instant.now());
        }
    }

    private PaymentTransaction requireMine(String transToken) {
        UUID actor = AuthSupport.requireUserId();
        PaymentTransaction tx = repository.findByTransToken(transToken)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (!tx.getPayerUserId().equals(actor) && !permissionService.hasStaffPermission(actor, true, StaffPermission.FINANCE)) {
            throw ApiException.notFound("Payment not found");
        }
        return tx;
    }

    private static String newCompanyRef() {
        return "COVE-" + Instant.now().toString().substring(0, 10).replace("-", "") + "-"
                + String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private PaymentResponse map(PaymentTransaction t, String paymentUrl) {
        String planName = planService.getOrThrow(t.getPlanId()).getName();
        String subjectName = t.getSubjectType() == SubscriptionSubjectType.ORGANISATION
                ? organisationRepository.findById(t.getSubjectId()).map(o -> o.getName()).orElse("Company")
                : "Personal";
        Instant subscriptionEnds = t.getSubscriptionId() == null ? null
                : subscriptionRepository.findById(t.getSubscriptionId()).map(Subscription::getExpiresAt).orElse(null);
        List<MethodOption> methods = Arrays.stream(PaymentMethod.values())
                .map(m -> new MethodOption(m.name(), m.label(), m == PaymentMethod.CARD ? "CARD" : "MOBILE_MONEY"))
                .toList();
        String payerEmail = userRepository.findById(t.getPayerUserId()).map(u -> u.getEmail()).orElse(null);
        return new PaymentResponse(t.getId(), t.getTransToken(), t.getCompanyRef(), paymentUrl, t.getStatus().name(),
                t.getAmount(), t.getCurrency(), t.getAmount().signum() == 0 ? "Free" : SubscriptionPlanService.formatBwp(t.getAmount()),
                t.getDescription(), planName, t.getSubjectType().name(), subjectName,
                t.getMethod() == null ? null : t.getMethod().label(), t.getApprovalRef(), t.getFailureReason(),
                t.getCreatedAt(), t.getExpiresAt(), t.getCompletedAt(), subscriptionEnds, t.getGateway(),
                "demo".equals(t.getGateway()), methods, payerEmail);
    }
}
