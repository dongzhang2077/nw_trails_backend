package ca.douglas.csis4280.nwtrails.domain;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "routes")
public record RoutePlan(
    @Id String id,
    String name,
    double distanceKm,
    int durationMinutes,
    String difficulty,
    List<String> landmarkIds
) {}
