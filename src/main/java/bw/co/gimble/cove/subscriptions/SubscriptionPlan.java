package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A named, reusable bundle of {@link SubscriptionFunction}s that can be sold/granted
 * to either an Organisation or an Individual, priced in BWP per billing period.
 * A price of 0 is a free plan (activates without payment).
 */
@Getter
@Setter
@Entity
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends Auditable {

    /** Short, stable, human-referenceable key e.g. "AGENCY_STARTER". Never shown to end users directly. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Which kind of subject this plan can be attached to. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionSubjectType subjectType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_functions", joinColumns = @JoinColumn(name = "plan_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "function", nullable = false)
    @Builder.Default
    private Set<SubscriptionFunction> functions = new HashSet<>();

    /**
     * Org plans only - caps how many active org members can operate under this plan's
     * unlocked functions. Null = unlimited. Ignored for INDIVIDUAL plans.
     */
    private Integer maxEmployees;

    /** Price per billing period in BWP. Null only on plans created before pricing existed. */
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal price;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BWP";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period")
    @Builder.Default
    private BillingPeriod billingPeriod = BillingPeriod.MONTHLY;

    /** Optional marketing note shown under the price, e.g. "First month free". */
    private String priceDescription;

    public boolean isFree() {
        return price != null && price.signum() == 0;
    }

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
