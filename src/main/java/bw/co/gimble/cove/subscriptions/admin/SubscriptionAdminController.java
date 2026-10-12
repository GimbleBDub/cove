package bw.co.gimble.cove.subscriptions.admin;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.subscriptions.FreeAccessCodeService;
import bw.co.gimble.cove.subscriptions.SubscriptionPlanService;
import bw.co.gimble.cove.subscriptions.SubscriptionService;
import bw.co.gimble.cove.subscriptions.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Platform-admin-only surface. Every method re-checks isPlatformAdmin() in the service
 * layer (not just here) - controllers are a thin pass-through in this codebase, the
 * permission gate always lives in the service so it can't be bypassed by another caller.
 */
@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class SubscriptionAdminController {

    private final SubscriptionPlanService planService;
    private final SubscriptionService subscriptionService;
    private final FreeAccessCodeService codeService;

    // --- Plans ---

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.adminListAll()));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(@Valid @RequestBody UpsertPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(planService.adminCreate(request)));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable UUID planId, @Valid @RequestBody UpsertPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planService.adminUpdate(planId, request)));
    }

    @PostMapping("/plans/{planId}/deactivate")
    public ResponseEntity<ApiResponse<PlanResponse>> deactivatePlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(ApiResponse.ok(planService.adminSetActive(planId, false)));
    }

    @PostMapping("/plans/{planId}/activate")
    public ResponseEntity<ApiResponse<PlanResponse>> activatePlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(ApiResponse.ok(planService.adminSetActive(planId, true)));
    }

    // --- Subscriptions (org-based and individual-based, every subject) ---

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.adminListAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> assign(@Valid @RequestBody AssignSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(subscriptionService.adminAssign(request)));
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable UUID subscriptionId) {
        subscriptionService.adminCancel(subscriptionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // --- Free access codes ---

    @GetMapping("/codes")
    public ResponseEntity<ApiResponse<List<AccessCodeResponse>>> listCodes() {
        return ResponseEntity.ok(ApiResponse.ok(codeService.adminList()));
    }

    @PostMapping("/codes")
    public ResponseEntity<ApiResponse<AccessCodeResponse>> createCode(@Valid @RequestBody CreateAccessCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(codeService.adminCreate(request)));
    }

    @PostMapping("/codes/{codeId}/deactivate")
    public ResponseEntity<ApiResponse<AccessCodeResponse>> deactivateCode(@PathVariable UUID codeId) {
        return ResponseEntity.ok(ApiResponse.ok(codeService.adminSetActive(codeId, false)));
    }

    @PostMapping("/codes/{codeId}/activate")
    public ResponseEntity<ApiResponse<AccessCodeResponse>> activateCode(@PathVariable UUID codeId) {
        return ResponseEntity.ok(ApiResponse.ok(codeService.adminSetActive(codeId, true)));
    }
}
