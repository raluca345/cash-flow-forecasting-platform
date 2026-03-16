package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.auth.AuthResponse;
import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.dtos.auth.AuthRequest;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .companyInviteCode(request.getCompanyInviteCode())
                .build();

        User user = userService.create(createReq);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("companyId", user.getCompany().getId());

        String token = jwtService.generateToken(claims, user);

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        User user = userService.getByEmail(request.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("companyId", user.getCompany().getId());

        String token = jwtService.generateToken(claims, user);
        return AuthResponse.builder().token(token).role(user.getRole().name()).build();
    }
}
