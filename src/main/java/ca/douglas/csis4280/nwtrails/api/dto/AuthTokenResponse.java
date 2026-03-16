package ca.douglas.csis4280.nwtrails.api.dto;

public record AuthTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserSummaryResponse user
) {}
