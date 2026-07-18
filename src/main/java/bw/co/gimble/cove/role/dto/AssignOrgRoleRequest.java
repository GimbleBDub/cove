package bw.co.gimble.cove.role.dto;

import bw.co.gimble.cove.role.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** e.g. AGENT, PROPERTY_MANAGER, ORG_STAFF, AGENCY_ADMIN - granted by an org admin. */
public record AssignOrgRoleRequest(
        @NotNull UUID userId,
        @NotNull Role role
) {}