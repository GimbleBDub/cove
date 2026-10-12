package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.organisations.JoinRequestStatus;
import bw.co.gimble.cove.role.Role;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestResponse(
        UUID id,
        UUID organisationId,
        String organisationName,
        UUID userId,
        Role requestedRole,
        String message,
        JoinRequestStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason,
        Instant createdAt,
        String userEmail,
        String userName
) {}