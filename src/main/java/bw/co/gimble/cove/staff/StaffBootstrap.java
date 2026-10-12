package bw.co.gimble.cove.staff;

import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform admins created before staff permissions existed have none, which would
 * lock them out of the back office. If no active super admin exists, give every
 * permission-less platform admin SUPER_ADMIN so there is always a way in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffBootstrap implements ApplicationRunner {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var staff = userRoleRepository.findAll().stream()
                .filter(r -> r.getRole() == Role.PLATFORM_ADMIN && r.getOrganisationId() == null && r.getStatus() == RoleStatus.ACTIVE)
                .map(r -> userRepository.findById(r.getUserId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        boolean hasSuper = staff.stream().anyMatch(u -> u.getStaffPermissions().contains(StaffPermission.SUPER_ADMIN));
        if (hasSuper) return;
        staff.stream()
                .filter(u -> u.getStaffPermissions().isEmpty())
                .forEach(u -> {
                    u.getStaffPermissions().add(StaffPermission.SUPER_ADMIN);
                    log.warn("Granted SUPER_ADMIN to existing platform admin {} (no super admin existed)", u.getEmail());
                });
    }
}
