package bw.co.gimble.cove.listings;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import bw.co.gimble.cove.listings.dto.SharePackResponse;
import bw.co.gimble.cove.listings.dto.UpsertListingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponse>>> search(
            @RequestParam(required = false) ListingPurpose purpose,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) BigDecimal minSizeSqm,
            @RequestParam(required = false) BigDecimal maxSizeSqm,
            @RequestParam(required = false) ListingSort sort,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID areaId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                listingService.search(purpose, typeId, city, suburb, minPrice, maxPrice, featured,
                        minBedrooms, minSizeSqm, maxSizeSqm, sort, cityId, areaId)));
    }

    /** Everything the signed-in user has listed, in any status (pending, rejected...). */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(listingService.mine()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.get(id)));
    }

    @GetMapping("/{id}/share-pack")
    public ResponseEntity<ApiResponse<SharePackResponse>> sharePack(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.sharePack(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> publish(@Valid @RequestBody UpsertListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(listingService.upsertAndPublish(request)));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<ListingResponse>> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.unpublish(id)));
    }
}
