package bw.co.gimble.cove.listings;

public enum ListingStatus {
    /** Live on the marketplace. */
    PUBLISHED,
    /** Waiting for a Cove moderator (lister is an individual or an unverified company). */
    PENDING_REVIEW,
    /** A moderator declined it; see reviewNote. The lister can fix and resubmit. */
    REJECTED,
    /** Was live, removed by a moderator; see reviewNote. Resubmitting goes back to review. */
    TAKEN_DOWN,
    /** Withdrawn by the lister. */
    UNPUBLISHED,
    /** Let or sold. */
    CLOSED
}
