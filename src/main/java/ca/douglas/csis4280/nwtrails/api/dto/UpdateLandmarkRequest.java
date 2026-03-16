package ca.douglas.csis4280.nwtrails.api.dto;

import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateLandmarkRequest(
    @NotBlank String name,
    @NotNull LandmarkCategory category,
    @NotBlank String address,
    @NotBlank String description,
    @NotNull Double latitude,
    @NotNull Double longitude,
    @NotBlank String imageUrl,
    @NotNull @DecimalMin("0.0") @DecimalMax("5.0") Double rating
) {}
