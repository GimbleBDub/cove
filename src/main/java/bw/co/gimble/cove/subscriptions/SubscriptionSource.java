package bw.co.gimble.cove.subscriptions;

/**
 * ADMIN_GRANT   - a platform admin assigned it directly (support case, manual sale, etc.)
 * FREE_CODE     - redeemed via a FreeAccessCode.
 * PAID          - bought through checkout (see payments.PaymentService).
 */
public enum SubscriptionSource {
    ADMIN_GRANT,
    FREE_CODE,
    PAID
}
