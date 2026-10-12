package bw.co.gimble.cove.events;

import java.util.UUID;

public record LeaseSignedEvent(UUID leaseId, UUID propertyId, UUID applicationId, UUID tenantUserId) {}
