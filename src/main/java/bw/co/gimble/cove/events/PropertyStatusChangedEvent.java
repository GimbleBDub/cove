package bw.co.gimble.cove.events;

import java.util.UUID;

public record PropertyStatusChangedEvent(UUID propertyId, String fromStatus, String toStatus, UUID actorUserId) {}
