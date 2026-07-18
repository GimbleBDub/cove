package bw.co.gimble.cove.role.dto;

import bw.co.gimble.cove.role.Role;
import jakarta.validation.constraints.NotNull;

/** e.g. OWNER, TENANT, BUYER - roles that don't require an organisation. */
public record AssignPersonalRoleRequest(
        @NotNull Role role
) {}