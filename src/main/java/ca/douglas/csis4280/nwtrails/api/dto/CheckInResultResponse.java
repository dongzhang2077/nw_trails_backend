package ca.douglas.csis4280.nwtrails.api.dto;

import ca.douglas.csis4280.nwtrails.domain.CheckInRecord;

public record CheckInResultResponse(
    String status,
    String message,
    CheckInRecord checkIn,
    BadgeProgressResponse badgeProgress,
    RouteProgressResponse routeProgress
) {}
