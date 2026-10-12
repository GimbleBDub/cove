package bw.co.gimble.cove.leads.dto;

import bw.co.gimble.cove.leads.LeadStage;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStageRequest(@NotNull LeadStage stage) {}
