package bw.co.gimble.cove.user;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;


    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean phoneVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private int tokenVersion = 0;


    /** Only meaningful for staff (users with the global PLATFORM_ADMIN role). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_staff_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    @Builder.Default
    private java.util.Set<bw.co.gimble.cove.staff.StaffPermission> staffPermissions = new java.util.HashSet<>();

    /** Set for accounts created with a temporary password (staff, invited company members). */
    @Column(name = "password_change_required", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean passwordChangeRequired = false;
}
