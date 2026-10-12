package bw.co.gimble.cove.subscriptions;

/**
 * A single unlockable capability. Plans are just named bundles of these (see
 * SubscriptionPlan#functions), so adding a new capability later (e.g. AUCTIONS)
 * never requires touching the plan/subscription schema - just add the enum value
 * and start referencing it from the relevant gating check.
 */
public enum SubscriptionFunction {
    PROPERTY_MANAGEMENT,
    AGENCY,
    VALUATION,
    AUCTIONS,
    MARKETPLACE_HIGHLIGHT
}
