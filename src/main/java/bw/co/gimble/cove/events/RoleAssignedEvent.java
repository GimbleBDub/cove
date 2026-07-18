package bw.co.gimble.cove.events;

import bw.co.gimble.cove.role.Role;

import java.util.UUID;

public record RoleAssignedEvent(
        UUID userRoleId,
        UUID userId,
        Role role,
        UUID organisationId,
        String organisationName,
        UUID grantedByUserId
) {}