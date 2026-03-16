package ca.douglas.csis4280.nwtrails.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCheckInRequest(
    @NotBlank String landmarkId,
    @NotNull Double latitude,
    @NotNull Double longitude,
    String note
) {}
