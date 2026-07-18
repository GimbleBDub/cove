package bw.co.gimble.cove.user;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identity only. Deliberately holds NO role/organisations fields - a user's access is
 * entirely described by their UserRole rows, since one person can hold
 * different roles at different organisations (or none).
 */
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

    /** Bumped on password change / forced logout so old JWTs can be rejected. */
    @Column(nullable = false)
    @Builder.Default
    private int tokenVersion = 0;

    @Column(nullable = false)
    private boolean verified = false;

}