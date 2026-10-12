package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.properties.dto.CreatePropertyRequest;
import bw.co.gimble.cove.properties.dto.OwnerSplitRequest;
import bw.co.gimble.cove.properties.dto.PropertyResponse;
import bw.co.gimble.cove.properties.dto.UpdatePropertyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> create(@Valid @RequestBody CreatePropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(propertyService.create(request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.mine()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePropertyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.update(id, request)));
    }

    @PutMapping("/{id}/owners")
    public ResponseEntity<ApiResponse<PropertyResponse>> replaceOwners(
            @PathVariable UUID id, @Valid @RequestBody List<OwnerSplitRequest> owners) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.replaceOwners(id, owners)));
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyResponse>> addMedia(
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(propertyService.addMedia(id, file)));
    }

    @PutMapping("/{id}/media/order")
    public ResponseEntity<ApiResponse<PropertyResponse>> reorderMedia(
            @PathVariable UUID id, @RequestBody List<UUID> orderedIds) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.reorderMedia(id, orderedIds)));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable UUID id, @PathVariable UUID mediaId) {
        propertyService.deleteMedia(id, mediaId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
