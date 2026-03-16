package ca.douglas.csis4280.nwtrails.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateRouteRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.0") Double distanceKm,
    @NotNull @jakarta.validation.constraints.Min(1) Integer durationMinutes,
    @NotBlank String difficulty,
    @NotEmpty List<String> landmarkIds
) {}
