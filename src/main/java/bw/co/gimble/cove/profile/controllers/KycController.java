package bw.co.gimble.cove.profile.controllers;

import bw.co.gimble.cove.profile.dto.kyc.CreateKycRequest;
import bw.co.gimble.cove.profile.dto.kyc.KycResponse;
import bw.co.gimble.cove.profile.dto.kyc.KycSummaryResponse;
import bw.co.gimble.cove.profile.dto.kyc.UpdateKycRequest;
import bw.co.gimble.cove.profile.services.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public KycResponse create(
            @Valid @RequestBody CreateKycRequest request) {
        return kycService.create(request);
    }

    @PutMapping("/{id}")
    public KycResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKycRequest request) {
        return kycService.update(id, request);
    }

    @GetMapping("/status")
    public KycSummaryResponse summary() {
        return kycService.getSummary();
    }
}
