package bw.co.gimble.cove.subscriptions.dto;

import bw.co.gimble.cove.subscriptions.SubscriptionFunction;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MyAccessResponse(
        /** The user's own INDIVIDUAL subscription, if any. Null = unsubscribed individually. */
        SubscriptionResponse individualSubscription,
        /** One entry per organisation the user belongs to that has an active subscription. */
        List<OrgAccess> organisations,
        /** Union of everything unlocked - individual plan's functions + every org's plan's functions. */
        Set<SubscriptionFunction> unlockedFunctions
) {
    public record OrgAccess(
            UUID organisationId,
            String organisationName,
            SubscriptionResponse subscription
    ) {}
}
