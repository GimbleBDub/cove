package bw.co.gimble.cove.events;

import java.util.UUID;

/** A user asked to join an organisation. Listeners typically notify that org's admins. */
public record OrganisationJoinRequestedEvent(
        UUID joinRequestId,
        UUID organisationId,
        String organisationName,
        UUID requesterUserId
) {}