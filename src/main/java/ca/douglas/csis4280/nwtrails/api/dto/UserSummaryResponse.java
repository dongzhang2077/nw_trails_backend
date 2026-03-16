package ca.douglas.csis4280.nwtrails.api.dto;

import java.util.List;

public record UserSummaryResponse(
    String userId,
    String username,
    String displayName,
    List<String> roles
) {}
