package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.role.Role;

public record RequestJoinOrganisationRequest(
        /** Optional hint of the role the requester expects, e.g. AGENT. Admin has final say. */
        Role requestedRole,
        String message
) {}