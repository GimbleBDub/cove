package bw.co.gimble.cove.subscriptions;

/**
 * A subscription's subjectId points at either a User (individual) or an
 * Organisation. Keeping this as a discriminator on one Subscription table -
 * rather than two separate tables - is what lets admin queries answer
 * "everything, from every org, and every person" with a single scan.
 */
public enum SubscriptionSubjectType {
    ORGANISATION,
    INDIVIDUAL
}
