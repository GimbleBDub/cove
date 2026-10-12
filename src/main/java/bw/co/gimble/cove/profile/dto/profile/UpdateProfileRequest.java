package bw.co.gimble.cove.profile.dto.profile;

import bw.co.gimble.cove.profile.enums.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateProfileRequest(

        @NotBlank
        String firstName,

        String middleName,

        @NotBlank
        String lastName,

        String preferredName,

        // Required for regular users, optional for platform admins - enforced in ProfileService.
        Gender gender,

        LocalDate dateOfBirth,

        String nationalId,

        String passportNumber,

        String nationality,

        String occupation,

        String profilePhoto,

        String bio,

        String addressLine1,

        String addressLine2,

        String city,

        String district,

        String country,

        String postalCode
) {
}