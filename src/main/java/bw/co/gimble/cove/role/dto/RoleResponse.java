package bw.co.gimble.cove.role.dto;

import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID userId,
        Role role,
        UUID organisationId,
        String organisationName,
        RoleStatus status,
        UUID grantedByUserId,
        Instant grantedAt,
        Instant revokedAt
) {}