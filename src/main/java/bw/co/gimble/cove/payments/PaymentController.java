package bw.co.gimble.cove.payments;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.payments.PaymentDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Step 1 (DPO createToken): returns the token and the checkout URL. */
    @PostMapping("/api/payments/checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(paymentService.checkout(request)));
    }

    @GetMapping("/api/payments/mine")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.mine()));
    }

    @GetMapping("/api/payments/{token}")
    public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.get(token)));
    }

    /** Step 2 (demo only): the checkout page reports the simulated outcome. */
    @PostMapping("/api/payments/{token}/demo-complete")
    public ResponseEntity<ApiResponse<PaymentResponse>> demoComplete(@PathVariable String token, @Valid @RequestBody DemoCompleteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.demoComplete(token, request)));
    }

    @PostMapping("/api/payments/{token}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancel(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.cancel(token)));
    }

    /** Step 3 (DPO verifyToken). */
    @GetMapping("/api/payments/{token}/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verify(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verify(token)));
    }

    @GetMapping("/api/admin/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.all()));
    }
}
