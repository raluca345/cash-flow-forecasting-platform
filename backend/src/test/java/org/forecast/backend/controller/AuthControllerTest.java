package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.forecast.backend.dtos.auth.AuthResponse;
import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.service.AuthService;
import org.forecast.backend.service.JwtService;
import org.forecast.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTestWithTestSecurity
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void signup_withInvite_returnsToken() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setName("Alice");
        req.setEmail("alice@test");
        req.setPassword("password1");
        req.setCompanyInviteCode("INV12345");

        when(authService.signup(req)).thenReturn(AuthResponse.builder().token("jwt-abc").build());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-abc"));
    }
}
