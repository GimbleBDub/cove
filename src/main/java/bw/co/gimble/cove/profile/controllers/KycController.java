package bw.co.gimble.cove.profile.controllers;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.profile.dto.kyc.*;
import bw.co.gimble.cove.profile.services.KycService;
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
@RequestMapping("/api/profile/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<KycResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(kycService.getMyDocuments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KycResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.get(id)));
    }

    /**
     * Multipart: "file" part is the document itself (jpg/png/pdf), "metadata" part is
     * a JSON body matching CreateKycRequest.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycResponse>> create(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") CreateKycRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(kycService.create(file, request)));
    }

    /** "file" part is optional here - omit it to update metadata only and keep the existing document. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycResponse>> update(
            @PathVariable UUID id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("metadata") UpdateKycRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.update(id, file, request)));
    }

    /** Owners can withdraw a document while it is still awaiting review. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        kycService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(kycService.getSummary()));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<KycResponse>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(kycService.listPending()));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<KycResponse>> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.verify(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<KycResponse>> reject(@PathVariable UUID id, @RequestBody(required = false) ReviewKycRequest request) {
        var reason = request != null ? request.rejectionReason() : null;
        return ResponseEntity.ok(ApiResponse.ok(kycService.reject(id, reason)));
    }
}
