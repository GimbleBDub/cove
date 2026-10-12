package bw.co.gimble.cove.admin;

import bw.co.gimble.cove.admin.AdminDtos.*;
import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.listings.ListingStatus;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Cove back office. Each service method checks the caller's staff permissions. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ListingService listingService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.dashboard()));
    }

    // --- Listings moderation ---

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> listings(@RequestParam(required = false) ListingStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.moderationQueue(status)));
    }

    @PostMapping("/listings/{id}/approve")
    public ResponseEntity<ApiResponse<ListingResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.approve(id)));
    }

    @PostMapping("/listings/{id}/reject")
    public ResponseEntity<ApiResponse<ListingResponse>> reject(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.reject(id, body == null ? null : body.reason())));
    }

    @PostMapping("/listings/{id}/take-down")
    public ResponseEntity<ApiResponse<ListingResponse>> takeDown(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.takeDown(id, body == null ? null : body.reason())));
    }

    @PostMapping("/listings/{id}/restore")
    public ResponseEntity<ApiResponse<ListingResponse>> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(listingService.restore(id)));
    }

    // --- Users ---

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> users(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.users(q)));
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest body) {
        adminService.setUserSuspended(id, true, body == null ? null : body.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        adminService.setUserSuspended(id, false, null);
        return ResponseEntity.noContent().build();
    }

    // --- Companies ---

    @GetMapping("/organisations")
    public ResponseEntity<ApiResponse<List<AdminOrganisationResponse>>> organisations(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.organisations(status)));
    }

    // --- Audit ---

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<List<AuditEntryResponse>>> audit(
            @RequestParam(required = false) String entityType, @RequestParam(required = false) UUID entityId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.audit(entityType, entityId)));
    }
}
