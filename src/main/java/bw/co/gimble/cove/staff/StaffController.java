package bw.co.gimble.cove.staff;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.staff.dto.StaffRequests.CreateStaffRequest;
import bw.co.gimble.cove.staff.dto.StaffRequests.EnabledRequest;
import bw.co.gimble.cove.staff.dto.StaffRequests.PermissionsRequest;
import bw.co.gimble.cove.staff.dto.StaffResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(staffService.create(request)));
    }

    @PutMapping("/{userId}/permissions")
    public ResponseEntity<ApiResponse<StaffResponse>> permissions(@PathVariable UUID userId, @Valid @RequestBody PermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.setPermissions(userId, request.permissions())));
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<StaffResponse>> resetPassword(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.resetPassword(userId)));
    }

    @PutMapping("/{userId}/enabled")
    public ResponseEntity<ApiResponse<StaffResponse>> enabled(@PathVariable UUID userId, @Valid @RequestBody EnabledRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.setEnabled(userId, request.enabled(), request.reason())));
    }
}
