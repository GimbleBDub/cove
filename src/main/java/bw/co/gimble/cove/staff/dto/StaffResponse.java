package bw.co.gimble.cove.staff.dto;

import bw.co.gimble.cove.staff.StaffPermission;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** [temporaryPassword] is only ever filled on create/reset, and shown once. */
public record StaffResponse(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        Set<StaffPermission> permissions,
        boolean enabled,
        boolean passwordChangeRequired,
        Instant createdAt,
        String temporaryPassword
) {}
