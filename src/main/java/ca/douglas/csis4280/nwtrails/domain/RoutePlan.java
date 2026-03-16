package ca.douglas.csis4280.nwtrails.domain;

import java.util.List;

public record RoutePlan(
    String id,
    String name,
    double distanceKm,
    int durationMinutes,
    String difficulty,
    List<String> landmarkIds
) {}
