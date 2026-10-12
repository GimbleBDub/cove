package bw.co.gimble.cove.events;

import java.util.UUID;

public record ApplicationSubmittedEvent(UUID applicationId, UUID listingId, UUID applicantUserId) {}
