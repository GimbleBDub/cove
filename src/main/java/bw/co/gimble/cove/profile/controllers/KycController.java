package bw.co.gimble.cove.profile.controllers;

import bw.co.gimble.cove.profile.dto.kyc.*;
import bw.co.gimble.cove.profile.services.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    public List<KycResponse> list() {
        return kycService.getMyDocuments();
    }

    @GetMapping("/{id}")
    public KycResponse get(@PathVariable UUID id) {
        return kycService.get(id);
    }

    /**
     * Multipart: "file" part is the document itself (jpg/png/pdf), "metadata" part is
     * a JSON body matching CreateKycRequest.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycResponse create(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") CreateKycRequest request) {
        return kycService.create(file, request);
    }

    /** "file" part is optional here - omit it to update metadata only and keep the existing document. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycResponse update(
            @PathVariable UUID id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("metadata") UpdateKycRequest request) {
        return kycService.update(id, file, request);
    }

    @GetMapping("/status")
    public KycSummaryResponse summary() {
        return kycService.getSummary();
    }

    @GetMapping("/admin/pending")
    public List<KycResponse> pending() {
        return kycService.listPending();
    }

    @PostMapping("/{id}/verify")
    public KycResponse verify(@PathVariable UUID id) {
        return kycService.verify(id);
    }

    @PostMapping("/{id}/reject")
    public KycResponse reject(@PathVariable UUID id, @RequestBody(required = false) ReviewKycRequest request) {
        var reason = request != null ? request.rejectionReason() : null;
        return kycService.reject(id, reason);
    }
}