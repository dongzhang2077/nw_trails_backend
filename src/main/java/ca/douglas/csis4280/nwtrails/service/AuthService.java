package ca.douglas.csis4280.nwtrails.service;

import ca.douglas.csis4280.nwtrails.api.dto.AuthTokenResponse;
import ca.douglas.csis4280.nwtrails.api.dto.UserSummaryResponse;
import ca.douglas.csis4280.nwtrails.common.ApiException;
import ca.douglas.csis4280.nwtrails.config.JwtProperties;
import ca.douglas.csis4280.nwtrails.domain.UserAccount;
import ca.douglas.csis4280.nwtrails.repository.UserAccountRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, RefreshTokenState> refreshTokenStore =
        new ConcurrentHashMap<>();

    public AuthService(
        JwtEncoder jwtEncoder,
        JwtProperties jwtProperties,
        UserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthTokenResponse login(String username, String password) {
        UserAccount user = findEnabledUser(username);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
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

        UserAccount user = findEnabledUser(state.username());

        refreshTokenStore.remove(refreshToken);
        return issueTokenPair(user);
    }

    public UserSummaryResponse me(String username) {
        UserAccount user = findEnabledUser(username);
        return toUserSummary(user);
    }

    public void logout(String username, String refreshToken) {
        RefreshTokenState state = refreshTokenStore.get(refreshToken);
        if (state == null) {
            return;
        }
        if (!state.username().equals(username)) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Refresh token does not belong to the current user."
            );
        }
        refreshTokenStore.remove(refreshToken);
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
            toUserSummary(user)
        );
    }

    private UserSummaryResponse toUserSummary(UserAccount user) {
        return new UserSummaryResponse(
            user.id(),
            user.username(),
            user.displayName(),
            user.roles()
        );
    }

    private UserAccount findEnabledUser(String username) {
        UserAccount user = userAccountRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Invalid username or password."
                )
            );

        if (!user.enabled()) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "User account is disabled."
            );
        }
        return user;
    }

    private String buildAccessToken(UserAccount user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet
            .builder()
            .issuer("nw-trails-backend")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.username())
            .claim("uid", user.id())
            .claim("displayName", user.displayName())
            .claim("roles", user.roles())
            .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder
            .encode(JwtEncoderParameters.from(jwsHeader, claims))
            .getTokenValue();
    }

    private record RefreshTokenState(String username, Instant expiresAt) {}
}
