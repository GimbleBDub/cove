package bw.co.gimble.cove.role;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.role.dto.GrantPlatformAdminRequest;
import bw.co.gimble.cove.role.dto.RoleResponse;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import bw.co.gimble.cove.common.ApiResponse;

import java.util.UUID;

/**
 * Only an existing platform admin can reach this - see UserRoleService#grantPlatformAdmin.
 * "Only those I add will be platform admins": there is intentionally no self-service or
 * bootstrap path here. The first platform admin is seeded directly in the database.
 */
@RestController
@RequestMapping("/api/admin/platform-admins")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final UserRoleService userRoleService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> grant(@Valid @RequestBody GrantPlatformAdminRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "No user with that email"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userRoleService.grantPlatformAdmin(user.getId())));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID userId) {
        userRoleService.revokePlatformAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}
