package bw.co.gimble.cove.payments;

public enum PaymentStatus {
    /** Token created, waiting for the customer at the checkout page. */
    PENDING,
    PAID,
    DECLINED,
    CANCELLED,
    /** Not completed within the token's lifetime. */
    EXPIRED
}
