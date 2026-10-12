package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RedeemCodeRequest(
        @NotBlank String code,
        @NotNull SubscriptionSubjectType subjectType,
        /** Required when subjectType=ORGANISATION. Ignored (defaults to self) when INDIVIDUAL. */
        UUID subjectId
) {}
