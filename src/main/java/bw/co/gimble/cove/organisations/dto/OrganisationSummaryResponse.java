package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.organisations.OrgType;
import bw.co.gimble.cove.organisations.OrgVerificationStatus;

import java.util.List;
import java.util.UUID;

public record OrganisationSummaryResponse(
        UUID id,
        String name,
        String slug,
        List<OrgType> types,
        OrgVerificationStatus verificationStatus
) {}