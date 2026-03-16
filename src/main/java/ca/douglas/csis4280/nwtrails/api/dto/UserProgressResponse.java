package ca.douglas.csis4280.nwtrails.api.dto;

import java.util.List;

public record UserProgressResponse(
    BadgeProgressResponse badgeProgress,
    List<CategoryProgressResponse> categoryProgress,
    RouteProgressResponse activeRoute
) {}
