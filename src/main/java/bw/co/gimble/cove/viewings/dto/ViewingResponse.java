package bw.co.gimble.cove.viewings.dto;

import bw.co.gimble.cove.viewings.ViewingStatus;

import java.time.Instant;
import java.util.UUID;

public record ViewingResponse(
        UUID id,
        UUID listingId,
        UUID leadId,
        Instant startsAt,
        Instant endsAt,
        ViewingStatus status,
        UUID requestedByUserId,
        UUID confirmedByUserId,
        Instant createdAt
) {}
