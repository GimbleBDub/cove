package bw.co.gimble.cove.payments;

import bw.co.gimble.cove.common.Auditable;
import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One checkout attempt, modelled on DPO Pay: Cove creates a transaction token
 * (DPO "TransToken"), sends the payer to the gateway's page, then verifies the
 * token to learn the result. Today the gateway is a demo (see DemoDpoGateway).
 */
@Getter
@Setter
@Entity
@Table(name = "payment_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_token", columnNames = "trans_token"),
        @UniqueConstraint(name = "uq_payment_company_ref", columnNames = "company_ref")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends Auditable {

    /** Gateway transaction token (DPO TransToken). */
    @Column(name = "trans_token", nullable = false)
    private String transToken;

    /** Cove's own reference (DPO CompanyRef), shown on receipts. */
    @Column(name = "company_ref", nullable = false)
    private String companyRef;

    @Column(name = "payer_user_id", nullable = false)
    private UUID payerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubscriptionSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    /** Gateway approval reference (DPO TransactionApproval) once paid. */
    @Column(name = "approval_ref")
    private String approvalRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** The subscription this payment activated. */
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    /** "demo" today; "dpo" once a live gateway is configured. */
    @Column(nullable = false)
    private String gateway;
}
