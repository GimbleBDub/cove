package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.properties.dto.PropertyTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/property-types")
@RequiredArgsConstructor
public class PropertyTypeController {

    private final PropertyTypeService propertyTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(propertyTypeService.listActive()));
    }
}
