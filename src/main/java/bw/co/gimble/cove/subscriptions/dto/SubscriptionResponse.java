package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionSource;
import bw.co.gimble.cove.subscriptions.SubscriptionStatus;
import bw.co.gimble.cove.subscriptions.SubscriptionSubjectType;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        SubscriptionSubjectType subjectType,
        UUID subjectId,
        /** Denormalised for admin screens - the org name or the user's display name, when resolvable. */
        String subjectLabel,
        PlanResponse plan,
        SubscriptionStatus status,
        SubscriptionSource source,
        Instant startsAt,
        Instant expiresAt,
        UUID grantedByUserId,
        String note
) {}
