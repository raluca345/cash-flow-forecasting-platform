package org.forecast.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.forecast.backend.dtos.auth.AuthRequest;
import org.forecast.backend.dtos.auth.AuthResponse;
import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.enums.Role;
import org.forecast.backend.exceptions.InviteExpiredException;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.forecast.backend.service.AuthService;
import org.forecast.backend.service.JwtService;
import org.forecast.backend.service.UserService;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTestWithTestSecurity
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private UserService userService;

  @Test
  void signup_withInvite_returnsToken() throws Exception {
    SignupRequest req = new SignupRequest();
    req.setName("Alice");
    req.setEmail("alice@test.com");
    req.setPassword("password1");
    req.setCompanyInviteCode("INV12345");

    when(authService.signup(any(SignupRequest.class)))
        .thenReturn(AuthResponse.builder().token("jwt-abc").role("FINANCE").build());

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-abc"))
        .andExpect(jsonPath("$.role").value("FINANCE"));
  }

  @Test
  void signup_invalidBody_returnsValidationError() throws Exception {
    SignupRequest req = new SignupRequest();
    req.setName("A");
    req.setEmail("not-an-email");
    req.setPassword("short");
    req.setCompanyInviteCode("BAD");

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.fieldErrors.name").value("User name must be between 2 and 255 characters"))
        .andExpect(jsonPath("$.fieldErrors.email").value("Email should be valid"))
        .andExpect(
            jsonPath("$.fieldErrors.password")
                .value("Password must be between 8 and 16 characters"))
        .andExpect(
            jsonPath("$.fieldErrors.companyInviteCode")
                .value("Invite code must be between 6 and 64 characters"));

    verifyNoInteractions(authService);
  }

  @Test
  void signup_inviteExpired_returns410() throws Exception {
    SignupRequest req = new SignupRequest();
    req.setName("Alice");
    req.setEmail("alice@test.com");
    req.setPassword("password1");
    req.setCompanyInviteCode("INV12345");

    when(authService.signup(any(SignupRequest.class)))
        .thenThrow(new InviteExpiredException("Company invite code has expired"));

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.error").value("INVITE_EXPIRED"))
        .andExpect(jsonPath("$.message").value("Company invite code has expired"));
  }

  @Test
  void signup_illegalArgument_returns400() throws Exception {
    SignupRequest req = new SignupRequest();
    req.setName("Alice");
    req.setEmail("alice@test.com");
    req.setPassword("password1");
    req.setCompanyInviteCode("INV12345");

    when(authService.signup(any(SignupRequest.class)))
        .thenThrow(new IllegalArgumentException("Email already in use"));

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
        .andExpect(jsonPath("$.message").value("Email already in use"));
  }

  @Test
  void login_returnsToken() throws Exception {
    AuthRequest req = new AuthRequest();
    req.setEmail("alice@test.com");
    req.setPassword("password1");

    when(authService.login(any(AuthRequest.class)))
        .thenReturn(AuthResponse.builder().token("jwt-login").role("FINANCE").build());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-login"))
        .andExpect(jsonPath("$.role").value("FINANCE"));
  }

  @Test
  void login_invalidBody_returnsValidationError() throws Exception {
    AuthRequest req = new AuthRequest();
    req.setEmail("not-an-email");
    req.setPassword("short");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.email").value("Email should be valid"))
        .andExpect(
            jsonPath("$.fieldErrors.password")
                .value("Password must be between 8 and 72 characters"));

    verifyNoInteractions(authService);
  }

  @Test
  void login_illegalArgument_returns400() throws Exception {
    AuthRequest req = new AuthRequest();
    req.setEmail("alice@test.com");
    req.setPassword("wrong-password");

    when(authService.login(any(AuthRequest.class)))
        .thenThrow(new IllegalArgumentException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void me_returnsCurrentUser() throws Exception {
    UUID companyId = UUID.randomUUID();

    Company company = new Company();
    company.setId(companyId);
    company.setName("Acme");
    company.setLogoUrl("/uploads/company.png");

    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Alice");
    user.setEmail("alice@test.com");
    user.setRole(Role.FINANCE);
    user.setCompany(company);

    when(userService.getByEmail("alice@test.com")).thenReturn(user);

    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .principal(new UsernamePasswordAuthenticationToken("alice@test.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("alice@test.com"))
        .andExpect(jsonPath("$.role").value("FINANCE"))
        .andExpect(jsonPath("$.company.id").value(companyId.toString()))
        .andExpect(jsonPath("$.company.name").value("Acme"));
  }
}
