package ca.douglas.csis4280.nwtrails.api.dto;

public record RouteProgressResponse(
    String routeId,
    String routeName,
    int completedStops,
    int totalStops,
    String nextStopLandmarkId,
    String nextStopName,
    boolean completed
) {}
