package bw.co.gimble.cove.locations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public final class LocationDtos {
    private LocationDtos() {}

    public record AreaResponse(UUID id, String name, boolean active) {}

    public record CityResponse(UUID id, String name, String district, boolean active, List<AreaResponse> areas) {}

    public record CityRequest(@NotBlank String name, String district, Boolean active) {}

    /** One or more area names at once, e.g. pasted from a list. */
    public record AddAreasRequest(@NotEmpty List<@NotBlank String> names) {}

    public record AreaRequest(@NotBlank String name, Boolean active) {}
}
