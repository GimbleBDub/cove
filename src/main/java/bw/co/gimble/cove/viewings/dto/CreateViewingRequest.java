package bw.co.gimble.cove.viewings.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateViewingRequest(
        @NotNull UUID listingId,
        UUID leadId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {}
