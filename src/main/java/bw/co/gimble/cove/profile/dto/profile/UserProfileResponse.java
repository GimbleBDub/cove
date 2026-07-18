package bw.co.gimble.cove.profile.dto.profile;

import bw.co.gimble.cove.profile.enums.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(

        UUID id,

        UUID userId,

        String firstName,

        String middleName,

        String lastName,

        String preferredName,

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

        String postalCode,

        Instant createdAt,

        Instant updatedAt
) {
}
