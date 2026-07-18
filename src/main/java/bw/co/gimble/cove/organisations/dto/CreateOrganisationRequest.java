package bw.co.gimble.cove.organisations.dto;

import bw.co.gimble.cove.organisations.OrgType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrganisationRequest(
        @NotBlank String name,
        @NotEmpty List<OrgType> types,
        String description,
        @Email String email,
        String phone,
        String website
) {}