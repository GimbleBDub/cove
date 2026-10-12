package bw.co.gimble.cove.leads.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLeadRequest(@NotNull UUID listingId, String message) {}
