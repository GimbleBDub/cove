package bw.co.gimble.cove.events;

import bw.co.gimble.cove.listings.ListingStatus;

import java.util.UUID;

/** A Cove moderator rejected or took down a listing. */
public record ListingModeratedEvent(UUID listingId, ListingStatus status, String reason) {}
