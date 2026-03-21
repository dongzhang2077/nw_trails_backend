package ca.douglas.csis4280.nwtrails.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "landmarks")
public record Landmark(
    @Id String id,
    String name,
    LandmarkCategory category,
    String address,
    String description,
    double latitude,
    double longitude,
    String imageUrl,
    double rating
) {}
