package ca.douglas.csis4280.nwtrails.service;

import ca.douglas.csis4280.nwtrails.api.dto.AuthTokenResponse;
import ca.douglas.csis4280.nwtrails.api.dto.UserSummaryResponse;
import ca.douglas.csis4280.nwtrails.common.ApiException;
import ca.douglas.csis4280.nwtrails.config.JwtProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Map<String, UserAccount> usersByUsername = Map.of(
        "student01",
        new UserAccount("u01", "student01", "Passw0rd!", "Dong Zhang", List.of("USER")),
        "admin01",
        new UserAccount(
            "u99",
            "admin01",
            "AdminPass!",
            "Group06 Admin",
            List.of("USER", "ADMIN")
        )
    );

    private final Map<String, RefreshTokenState> refreshTokenStore =
        new ConcurrentHashMap<>();

    public AuthService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public AuthTokenResponse login(String username, String password) {
        UserAccount user = usersByUsername.get(username);
        if (user == null || !user.password().equals(password)) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Invalid username or password."
            );
        }
        return issueTokenPair(user);
    }

    public AuthTokenResponse refresh(String refreshToken) {
        RefreshTokenState state = refreshTokenStore.get(refreshToken);
        if (state == null || state.expiresAt().isBefore(Instant.now())) {
            refreshTokenStore.remove(refreshToken);
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Refresh token is invalid or expired."
            );
        }

        UserAccount user = usersByUsername.get(state.username());
        if (user == null) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "User account is unavailable."
            );
        }

        refreshTokenStore.remove(refreshToken);
        return issueTokenPair(user);
    }

    private AuthTokenResponse issueTokenPair(UserAccount user) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(jwtProperties.getAccessTokenSeconds());
        String accessToken = buildAccessToken(user, now, accessExpiresAt);

        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        Instant refreshExpiresAt = now.plusSeconds(jwtProperties.getRefreshTokenSeconds());
        refreshTokenStore.put(refreshToken, new RefreshTokenState(user.username(), refreshExpiresAt));

        return new AuthTokenResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtProperties.getAccessTokenSeconds(),
            new UserSummaryResponse(user.userId(), user.username(), user.displayName(), user.roles())
        );
    }

    private String buildAccessToken(UserAccount user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet
            .builder()
            .issuer("nw-trails-backend")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.username())
            .claim("uid", user.userId())
            .claim("displayName", user.displayName())
            .claim("roles", user.roles())
            .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder
            .encode(JwtEncoderParameters.from(jwsHeader, claims))
            .getTokenValue();
    }

    private record RefreshTokenState(String username, Instant expiresAt) {}

    private record UserAccount(
        String userId,
        String username,
        String password,
        String displayName,
        List<String> roles
    ) {}
}
