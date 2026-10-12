package bw.co.gimble.cove.events;

import java.util.UUID;

public record ViewingBookedEvent(UUID viewingId, UUID listingId, UUID requestedByUserId) {}
