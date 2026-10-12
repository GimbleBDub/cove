package bw.co.gimble.cove.payments;

import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record CheckoutRequest(@NotNull UUID planId, @NotNull SubscriptionSubjectType subjectType, UUID subjectId) {}

    public record DemoCompleteRequest(@NotNull PaymentMethod method, @NotNull Boolean approve) {}

    public record MethodOption(String code, String label, String kind) {}

    public record PaymentResponse(
            UUID id,
            String transToken,
            String companyRef,
            String paymentUrl,
            String status,
            BigDecimal amount,
            String currency,
            String amountLabel,
            String description,
            String planName,
            String subjectType,
            String subjectName,
            String method,
            String approvalRef,
            String failureReason,
            Instant createdAt,
            Instant expiresAt,
            Instant completedAt,
            Instant subscriptionExpiresAt,
            String gateway,
            boolean demo,
            List<MethodOption> methods,
            String payerEmail
    ) {}

    public record VerifyResponse(String transToken, String resultCode, String explanation, String status) {}
}
