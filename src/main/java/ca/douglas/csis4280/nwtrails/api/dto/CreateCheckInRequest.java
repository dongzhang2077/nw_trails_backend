package ca.douglas.csis4280.nwtrails.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCheckInRequest(
    @NotBlank String landmarkId,
    @NotNull Double latitude,
    @NotNull Double longitude,
    String note,
    List<String> photoUrls
) {}
