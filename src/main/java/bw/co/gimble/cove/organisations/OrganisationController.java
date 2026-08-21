package bw.co.gimble.cove.organisations;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.organisations.dto.*;
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
@RequestMapping("/api/organisations")
@RequiredArgsConstructor
public class OrganisationController {

    private final OrganisationService organisationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganisationResponse>> create(@Valid @RequestBody CreateOrganisationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(organisationService.create(request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<OrganisationResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.myOrganisations()));
    }

    /** Discoverable-organisation search, e.g. GET /api/organisations/search?query=acme */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrganisationSummaryResponse>>> search(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.search(query)));
    }

    @GetMapping("/{organisationId}")
    public ResponseEntity<ApiResponse<OrganisationResponse>> get(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.get(organisationId)));
    }

    @PutMapping("/{organisationId}")
    public ResponseEntity<ApiResponse<OrganisationResponse>> update(
            @PathVariable UUID organisationId, @Valid @RequestBody UpdateOrganisationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.update(organisationId, request)));
    }

    /** Admin-only. */
    @PutMapping(value = "/{organisationId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrganisationResponse>> updateLogo(
            @PathVariable UUID organisationId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.updateLogo(organisationId, file)));
    }

    // ─── join requests ──────────────────────────────────────────

    @PostMapping("/{organisationId}/join-requests")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> requestToJoin(
            @PathVariable UUID organisationId, @Valid @RequestBody RequestJoinOrganisationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(organisationService.requestToJoin(organisationId, request)));
    }

    @DeleteMapping("/{organisationId}/join-requests/{requestId}")
    public ResponseEntity<Void> cancelJoinRequest(@PathVariable UUID organisationId, @PathVariable UUID requestId) {
        organisationService.cancelJoinRequest(organisationId, requestId);
        return ResponseEntity.noContent().build();
    }

    /** Admin-only: pending (or ?status=APPROVED/REJECTED/CANCELLED) requests for this organisation. */
    @GetMapping("/{organisationId}/join-requests")
    public ResponseEntity<ApiResponse<List<JoinRequestResponse>>> listJoinRequests(
            @PathVariable UUID organisationId, @RequestParam(required = false) JoinRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.listJoinRequests(organisationId, status)));
    }

    @GetMapping("/join-requests/mine")
    public ResponseEntity<ApiResponse<List<JoinRequestResponse>>> myJoinRequests() {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.myJoinRequests()));
    }

    @PostMapping("/{organisationId}/join-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> approve(
            @PathVariable UUID organisationId, @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewJoinRequestRequest request) {
        var role = request != null ? request.role() : null;
        return ResponseEntity.ok(ApiResponse.ok(organisationService.approve(organisationId, requestId, role)));
    }

    @PostMapping("/{organisationId}/join-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> reject(
            @PathVariable UUID organisationId, @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewJoinRequestRequest request) {
        var reason = request != null ? request.rejectionReason() : null;
        return ResponseEntity.ok(ApiResponse.ok(organisationService.reject(organisationId, requestId, reason)));
    }

    @GetMapping("/admin/pending-verification")
    public ResponseEntity<ApiResponse<List<OrganisationSummaryResponse>>> pendingVerification() {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.listPendingVerification()));
    }

    @PostMapping("/{organisationId}/verify")
    public ResponseEntity<ApiResponse<OrganisationResponse>> verify(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.verify(organisationId)));
    }

    @PostMapping("/{organisationId}/reject-verification")
    public ResponseEntity<ApiResponse<OrganisationResponse>> rejectVerification(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(organisationService.rejectVerification(organisationId)));
    }
}