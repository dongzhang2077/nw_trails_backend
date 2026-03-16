package ca.douglas.csis4280.nwtrails.domain;

public record Landmark(
    String id,
    String name,
    LandmarkCategory category,
    String address,
    String description,
    double latitude,
    double longitude,
    String imageUrl,
    double rating
) {}
