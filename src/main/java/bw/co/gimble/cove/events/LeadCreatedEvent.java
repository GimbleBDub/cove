package bw.co.gimble.cove.events;

import java.util.UUID;

public record LeadCreatedEvent(UUID leadId, UUID listingId, UUID inquirerUserId, UUID assignedAgentUserId) {}
