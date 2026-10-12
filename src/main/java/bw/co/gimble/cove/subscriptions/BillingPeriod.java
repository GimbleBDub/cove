package bw.co.gimble.cove.subscriptions;

import java.time.Instant;
import java.time.ZoneOffset;

public enum BillingPeriod {
    MONTHLY,
    YEARLY;

    /** When a subscription paid now for one period runs out. */
    public Instant endFrom(Instant start) {
        var at = start.atOffset(ZoneOffset.UTC);
        return (this == MONTHLY ? at.plusMonths(1) : at.plusYears(1)).toInstant();
    }

    public String suffix() {
        return this == MONTHLY ? "/ month" : "/ year";
    }
}
