package bw.co.gimble.cove.events;

import java.util.UUID;

public record OrganisationJoinReviewedEvent(
        UUID joinRequestId,
        UUID organisationId,
        String organisationName,
        UUID requesterUserId,
        boolean approved,
        String reason
) {}