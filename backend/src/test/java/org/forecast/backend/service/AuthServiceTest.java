package org.forecast.backend.service;

import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.User;
import org.forecast.backend.model.Company;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_success_returnsAuthResponse() {
        SignupRequest req = new SignupRequest();
        req.setName("Jane Doe");
        req.setEmail("jane@acme.test");
        req.setPassword("secret123");
        req.setCompanyInviteCode("INVITE123");

        User user = new User();
        user.setEmail("jane@acme.test");
        Company company = new Company();
        company.setId(UUID.randomUUID());
        user.setCompany(company);

        when(userService.create(any(CreateUserRequest.class))).thenReturn(user);
        // use lenient stubbing to avoid strict stubbing mismatches when the method is invoked with different overloads
        lenient().when(jwtService.generateToken(anyMap(), eq(user))).thenReturn("jwt-token-xyz");
        // also stub the single-arg overload in case the code calls the simpler method
        lenient().when(jwtService.generateToken(eq(user))).thenReturn("jwt-token-xyz");

        var resp = authService.signup(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("jwt-token-xyz");

        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).create(captor.capture());
        CreateUserRequest sent = captor.getValue();
        assertThat(sent.getName()).isEqualTo("Jane Doe");
        assertThat(sent.getEmail()).isEqualTo("jane@acme.test");
        // AuthService now forwards the raw password; UserService is responsible for encoding.
        assertThat(sent.getPassword()).isEqualTo("secret123");
        assertThat(sent.getCompanyInviteCode()).isEqualTo("INVITE123");
    }

    @Test
    void signup_companyNotFound_throwsResourceNotFound() {
        SignupRequest req = new SignupRequest();
        req.setName("Jim");
        req.setEmail("jim@noexist.test");
        req.setPassword("pw");
        req.setCompanyInviteCode("BADCODE");

        when(userService.create(any(CreateUserRequest.class))).thenThrow(new ResourceNotFoundException("No company"));

        assertThrows(ResourceNotFoundException.class, () -> authService.signup(req));

        verify(jwtService, never()).generateToken(any(User.class));
    }
}

