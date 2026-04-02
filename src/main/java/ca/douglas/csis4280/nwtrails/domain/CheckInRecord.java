package ca.douglas.csis4280.nwtrails.domain;

import java.time.Instant;
import java.util.List;

public record CheckInRecord(
    String id,
    String userId,
    String landmarkId,
    Instant checkedInAt,
    String note,
    List<String> photoUrls
) {}
