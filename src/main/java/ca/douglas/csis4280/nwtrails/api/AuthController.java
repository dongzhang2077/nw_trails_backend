package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.AuthTokenResponse;
import ca.douglas.csis4280.nwtrails.api.dto.LoginRequest;
import ca.douglas.csis4280.nwtrails.api.dto.LogoutRequest;
import ca.douglas.csis4280.nwtrails.api.dto.RefreshTokenRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UserSummaryResponse;
import ca.douglas.csis4280.nwtrails.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    public UserSummaryResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        Authentication authentication,
        @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(authentication.getName(), request.refreshToken());
    }
}
