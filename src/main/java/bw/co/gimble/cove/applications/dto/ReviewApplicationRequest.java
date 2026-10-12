package bw.co.gimble.cove.applications.dto;

import bw.co.gimble.cove.applications.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(@NotNull ApplicationStatus status) {}
