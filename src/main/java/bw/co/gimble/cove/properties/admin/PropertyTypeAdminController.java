package bw.co.gimble.cove.properties.admin;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.properties.PropertyTypeService;
import bw.co.gimble.cove.properties.dto.PropertyTypeResponse;
import bw.co.gimble.cove.properties.dto.UpsertPropertyTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/property-types")
@RequiredArgsConstructor
public class PropertyTypeAdminController {

    private final PropertyTypeService propertyTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(propertyTypeService.adminListAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> create(@Valid @RequestBody UpsertPropertyTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(propertyTypeService.adminCreate(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpsertPropertyTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(propertyTypeService.adminUpdate(id, request)));
    }
}
