package bw.co.gimble.cove.applications;

import bw.co.gimble.cove.applications.dto.ApplicationResponse;
import bw.co.gimble.cove.applications.dto.CreateApplicationRequest;
import bw.co.gimble.cove.applications.dto.LeaseResponse;
import bw.co.gimble.cove.applications.dto.ReviewApplicationRequest;
import bw.co.gimble.cove.applications.dto.SignTextRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import bw.co.gimble.cove.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(@Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(applicationService.submit(request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.mine()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.get(id)));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ApplicationResponse>> review(
            @PathVariable UUID id, @Valid @RequestBody ReviewApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.review(id, request)));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.withdraw(id)));
    }

    @PostMapping(value = "/{id}/sign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ApplicationResponse>> signText(
            @PathVariable UUID id, @Valid @RequestBody SignTextRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.sign(id, request.typedName(), null)));
    }

    @PostMapping(value = "/{id}/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicationResponse>> sign(
            @PathVariable UUID id,
            @RequestPart(value = "typedName", required = false) String typedName,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.sign(id, typedName, file)));
    }

    @GetMapping("/{id}/lease")
    public ResponseEntity<ApiResponse<LeaseResponse>> lease(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getLease(id)));
    }
}
