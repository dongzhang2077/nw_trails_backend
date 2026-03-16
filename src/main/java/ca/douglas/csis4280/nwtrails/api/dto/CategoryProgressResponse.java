package ca.douglas.csis4280.nwtrails.api.dto;

import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;

public record CategoryProgressResponse(
    LandmarkCategory category,
    int visitedCount,
    int totalCount
) {}
