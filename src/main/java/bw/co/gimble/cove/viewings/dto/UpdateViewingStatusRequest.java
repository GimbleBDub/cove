package bw.co.gimble.cove.viewings.dto;

import bw.co.gimble.cove.viewings.ViewingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateViewingStatusRequest(@NotNull ViewingStatus status) {}
