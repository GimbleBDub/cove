package bw.co.gimble.cove.leads.dto;

import bw.co.gimble.cove.leads.LeadStage;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID listingId,
        UUID inquirerUserId,
        UUID assignedAgentUserId,
        LeadStage stage,
        Instant createdAt
) {}
