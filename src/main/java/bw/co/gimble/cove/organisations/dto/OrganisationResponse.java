package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.organisations.OrgType;
import bw.co.gimble.cove.organisations.OrgVerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganisationResponse(
        UUID id,
        String name,
        String slug,
        List<OrgType> types,
        OrgVerificationStatus verificationStatus,
        String description,
        String email,
        String phone,
        String website,
        boolean discoverable,
        Instant createdAt
) {}