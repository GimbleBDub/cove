package bw.co.gimble.cove.profile.models;

import bw.co.gimble.cove.common.Auditable;
import bw.co.gimble.cove.profile.enums.Gender;
import bw.co.gimble.cove.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.stream.Stream;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends Auditable {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable =false)
    private String lastName;

    private String preferredName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dateOfBirth;

    private String nationalId;

    private String passportNumber;

    private String nationality;

    private String occupation;

    private String profilePhoto;

    private String bio;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String district;

    private String country;

    private String postalCode;

    @Column(nullable = false)
    private boolean firstCompletion = false;

    @Transient
    public String getFullName() {
        return Stream.of(firstName, middleName, lastName)
                .filter(part -> part != null && !part.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
