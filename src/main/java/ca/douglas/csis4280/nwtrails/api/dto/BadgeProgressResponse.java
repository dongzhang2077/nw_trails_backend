package ca.douglas.csis4280.nwtrails.api.dto;

public record BadgeProgressResponse(
    int visitedCount,
    int totalLandmarks,
    boolean bronzeEarned,
    boolean silverEarned,
    boolean goldEarned,
    String nextBadgeHint
) {}
