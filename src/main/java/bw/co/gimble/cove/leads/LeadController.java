package bw.co.gimble.cove.leads;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.leads.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> enquire(@Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(leadService.enquire(request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(leadService.mine()));
    }

    @PostMapping("/{id}/stage")
    public ResponseEntity<ApiResponse<LeadResponse>> updateStage(
            @PathVariable UUID id, @Valid @RequestBody UpdateLeadStageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.updateStage(id, request)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> messages(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.messages(id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(leadService.sendMessage(id, request)));
    }
}
