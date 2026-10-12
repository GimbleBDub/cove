package bw.co.gimble.cove.subscriptions.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateAccessCodeRequest(
        /** Optional - server generates one if omitted. */
        String code,
        @NotNull UUID planId,
        Integer durationDays,
        Integer maxRedemptions,
        Instant expiresAt,
        String note
) {}
