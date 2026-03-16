package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.auth.AuthRequest;
import org.forecast.backend.dtos.auth.AuthResponse;
import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.dtos.auth.UserMeResponse;
import org.forecast.backend.model.User;
import org.forecast.backend.service.AuthService;
import org.forecast.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request

    ) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(UserMeResponse.fromEntity(user));
    }
}
