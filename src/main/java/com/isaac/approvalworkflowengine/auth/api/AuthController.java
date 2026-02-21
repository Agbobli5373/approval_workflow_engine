package com.isaac.approvalworkflowengine.auth.api;

import com.isaac.approvalworkflowengine.auth.service.LocalAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "app.security", name = "mode", havingValue = "LOCAL_AUTH", matchIfMissing = true)
public class AuthController {

    private final LocalAuthenticationService localAuthenticationService;

    public AuthController(LocalAuthenticationService localAuthenticationService) {
        this.localAuthenticationService = localAuthenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and issue access token")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return localAuthenticationService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        localAuthenticationService.logout(jwt);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return localAuthenticationService.currentUser(jwt);
    }
}
