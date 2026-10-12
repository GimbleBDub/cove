package bw.co.gimble.cove.locations;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.locations.dto.LocationDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /** Public: active cities with their active areas, for pickers and filters. */
    @GetMapping("/api/locations")
    public ResponseEntity<ApiResponse<List<CityResponse>>> catalogue() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.publicCatalogue()));
    }

    @GetMapping("/api/admin/locations")
    public ResponseEntity<ApiResponse<List<CityResponse>>> adminCatalogue() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.adminCatalogue()));
    }

    @PostMapping("/api/admin/locations/cities")
    public ResponseEntity<ApiResponse<CityResponse>> createCity(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(locationService.createCity(request)));
    }

    @PutMapping("/api/admin/locations/cities/{cityId}")
    public ResponseEntity<ApiResponse<CityResponse>> updateCity(@PathVariable UUID cityId, @Valid @RequestBody CityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updateCity(cityId, request)));
    }

    @PostMapping("/api/admin/locations/cities/{cityId}/areas")
    public ResponseEntity<ApiResponse<CityResponse>> addAreas(@PathVariable UUID cityId, @Valid @RequestBody AddAreasRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.addAreas(cityId, request)));
    }

    @PutMapping("/api/admin/locations/areas/{areaId}")
    public ResponseEntity<ApiResponse<CityResponse>> updateArea(@PathVariable UUID areaId, @Valid @RequestBody AreaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updateArea(areaId, request)));
    }
}
