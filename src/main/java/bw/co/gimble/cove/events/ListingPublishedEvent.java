package bw.co.gimble.cove.events;

import bw.co.gimble.cove.listings.ListingPurpose;

import java.util.UUID;

public record ListingPublishedEvent(UUID listingId, UUID propertyId, ListingPurpose purpose, UUID actorUserId) {}
