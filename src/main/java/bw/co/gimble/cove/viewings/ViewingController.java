package bw.co.gimble.cove.viewings;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.viewings.dto.CreateViewingRequest;
import bw.co.gimble.cove.viewings.dto.UpdateViewingStatusRequest;
import bw.co.gimble.cove.viewings.dto.ViewingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/viewings")
@RequiredArgsConstructor
public class ViewingController {

    private final ViewingService viewingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ViewingResponse>> request(@Valid @RequestBody CreateViewingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(viewingService.request(request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ViewingResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(viewingService.mine()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ViewingResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateViewingStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(viewingService.updateStatus(id, request)));
    }
}
