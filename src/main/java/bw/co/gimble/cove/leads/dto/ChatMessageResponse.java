package bw.co.gimble.cove.leads.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(UUID id, UUID leadId, UUID senderUserId, String body, Instant createdAt) {}
