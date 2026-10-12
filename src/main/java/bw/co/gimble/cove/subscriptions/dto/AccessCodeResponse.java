package bw.co.gimble.cove.subscriptions.dto;

import java.time.Instant;
import java.util.UUID;

public record AccessCodeResponse(
        UUID id,
        String code,
        PlanResponse plan,
        Integer durationDays,
        Integer maxRedemptions,
        int redemptionCount,
        Instant expiresAt,
        boolean active,
        String note
) {}
