package bw.co.gimble.cove.role;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.role.dto.AssignOrgRoleRequest;
import bw.co.gimble.cove.role.dto.AssignPersonalRoleRequest;
import bw.co.gimble.cove.role.dto.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserRoleService userRoleService;

    /** All active roles held by the current user, across all organisations plus any personal ones. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(userRoleService.myRoles()));
    }

    /** Self-service: OWNER / TENANT / BUYER. No approval needed - you just start acting in that capacity. */
    @PostMapping("/personal")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPersonalRole(@Valid @RequestBody AssignPersonalRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userRoleService.assignPersonalRole(request.role())));
    }

    /** Org-admin only: grant AGENT / PROPERTY_MANAGER / ORG_STAFF / AGENCY_ADMIN to a member. */
    @PostMapping("/organisations/{organisationId}")
    public ResponseEntity<ApiResponse<RoleResponse>> assignOrgRole(
            @PathVariable UUID organisationId, @Valid @RequestBody AssignOrgRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userRoleService.assignOrgRole(organisationId, request.userId(), request.role())));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID roleId) {
        userRoleService.revokeRole(roleId);
        return ResponseEntity.noContent().build();
    }

    /** Members (or platform admin) can list who holds a role at an organisation. */
    @GetMapping("/organisations/{organisationId}")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listOrgMembers(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(userRoleService.listOrgMembers(organisationId)));
    }
}