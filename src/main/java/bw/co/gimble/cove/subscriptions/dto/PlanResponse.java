package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionFunction;
import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;

import java.util.Set;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        SubscriptionSubjectType subjectType,
        Set<SubscriptionFunction> functions,
        Integer maxEmployees,
        String priceDescription,
        boolean active,
        java.math.BigDecimal price,
        String currency,
        bw.co.gimble.cove.subscriptions.BillingPeriod billingPeriod,
        /** Ready to display, e.g. "BWP 450 / month", "Free" or "Price on request". */
        String priceLabel,
        boolean free
) {}
