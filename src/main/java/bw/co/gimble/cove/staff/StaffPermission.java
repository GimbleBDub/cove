package bw.co.gimble.cove.staff;

/**
 * What a Cove staff member (a user holding the global PLATFORM_ADMIN role) may do
 * in the back office. Staff never list properties; they monitor, verify and approve.
 */
public enum StaffPermission {
    /** Everything, including managing staff and reading the audit log. */
    SUPER_ADMIN,
    /** Identity (KYC) documents and company verification. */
    VERIFIER,
    /** Review, approve, reject and take down listings. */
    MODERATOR,
    /** Users, enquiries and viewings: suspend/reactivate accounts, help people. */
    SUPPORT,
    /** Plans, subscriptions and access codes. */
    FINANCE,
    /** Reference data: cities, areas and property types. */
    CATALOGUE,
    /** Read-only access to every area except staff management and the audit log. */
    VIEWER
}
