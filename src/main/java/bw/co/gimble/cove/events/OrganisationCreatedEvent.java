package bw.co.gimble.cove.events;

import java.util.UUID;

public record OrganisationCreatedEvent(
        UUID organisationId,
        String organisationName,
        UUID createdByUserId
) {}