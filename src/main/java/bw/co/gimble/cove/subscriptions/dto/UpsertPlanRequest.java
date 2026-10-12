package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionFunction;
import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpsertPlanRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull SubscriptionSubjectType subjectType,
        @NotEmpty Set<SubscriptionFunction> functions,
        Integer maxEmployees,
        @jakarta.validation.constraints.NotNull(message = "Set a price (0 for a free plan)")
        @jakarta.validation.constraints.DecimalMin(value = "0", message = "Price can't be negative")
        @jakarta.validation.constraints.Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        java.math.BigDecimal price,
        @jakarta.validation.constraints.NotNull(message = "Choose monthly or yearly billing")
        bw.co.gimble.cove.subscriptions.BillingPeriod billingPeriod,
        @jakarta.validation.constraints.Size(max = 120) String priceDescription
) {}
