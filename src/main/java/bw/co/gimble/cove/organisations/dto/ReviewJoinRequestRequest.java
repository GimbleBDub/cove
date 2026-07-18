package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.role.Role;

/**
 * Used for approvals (role required - the admin decides the final role, which
 * may differ from what the requester asked for) and rejections (rejectionReason optional).
 */
public record ReviewJoinRequestRequest(
        Role role,
        String rejectionReason
) {}