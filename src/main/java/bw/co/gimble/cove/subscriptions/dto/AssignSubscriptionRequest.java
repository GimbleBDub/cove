package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AssignSubscriptionRequest(
        @NotNull SubscriptionSubjectType subjectType,
        @NotNull UUID subjectId,
        @NotNull UUID planId,
        /** Null = does not expire on its own. */
        Instant expiresAt,
        String note
) {}
