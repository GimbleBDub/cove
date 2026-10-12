package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.subscriptions.dto.MyAccessResponse;
import bw.co.gimble.cove.subscriptions.dto.PlanResponse;
import bw.co.gimble.cove.subscriptions.dto.RedeemCodeRequest;
import bw.co.gimble.cove.subscriptions.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionPlanService planService;
    private final SubscriptionService subscriptionService;
    private final FreeAccessCodeService codeService;

    /** Available plans, for the onboarding subscription step and Settings > Subscription. */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> plans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.listActive()));
    }

    /** What the current user can see right now - their own plan plus every org they belong to. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<MyAccessResponse>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.myAccess()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> redeem(@Valid @RequestBody RedeemCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(codeService.redeem(request)));
    }
}
