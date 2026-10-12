package bw.co.gimble.cove.staff.dto;

import bw.co.gimble.cove.staff.StaffPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public final class StaffRequests {
    private StaffRequests() {}

    public record CreateStaffRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            @NotEmpty Set<StaffPermission> permissions
    ) {}

    public record PermissionsRequest(@NotEmpty Set<StaffPermission> permissions) {}

    public record EnabledRequest(@NotNull Boolean enabled, String reason) {}
}
