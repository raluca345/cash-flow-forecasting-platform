package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.forecast.backend.dtos.auth.AuthRequest;
import org.forecast.backend.dtos.auth.SignupRequest;
import org.forecast.backend.model.Company;
import org.forecast.backend.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    public void createObjectMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private Company createCompanyWithInvite() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Company c = new Company();
        c.setName("Integration Co " + suffix);
        c.setAddress("1 Test St");
        c.setEmail("billing+" + suffix + "@integration.test");
        c.setPhoneNumber("+1 555 0000");
        c.setWebsite("https://integration.test");
        String baseIban = "DE893704004405320140";
        String ibanSuffix = suffix.substring(0, Math.min(8, suffix.length()));
        c.setIban((baseIban + ibanSuffix).toUpperCase());
        c.setVatNumber(("VAT" + suffix).toUpperCase());
        c.setInviteCode("INTEGRATION-" + suffix);
        c.setInviteCodeExpiresAt(Instant.now().plusSeconds(3600));
        return companyRepository.save(c);
    }

    @Test
    void signup_returnsToken() throws Exception {
        Company company = createCompanyWithInvite();

        SignupRequest req = new SignupRequest();
        req.setName("Alice");
        req.setEmail("alice.integration@test");
        req.setPassword("password123");
        req.setCompanyInviteCode(company.getInviteCode());

        String content = objectMapper.writeValueAsString(req);

        var mvcResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        assertThat(body).contains("token");
    }

    @Test
    void login_afterSignup_returnsToken() throws Exception {
        Company company = createCompanyWithInvite();

        SignupRequest signup = new SignupRequest();
        signup.setName("Bob");
        signup.setEmail("bob.integration@test");
        signup.setPassword("password456");
        signup.setCompanyInviteCode(company.getInviteCode());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());

        AuthRequest login = new AuthRequest();
        login.setEmail(signup.getEmail());
        login.setPassword(signup.getPassword());

        var mvcResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        assertThat(body).contains("token");
    }

    @Test
    void signup_then_login_flow() throws Exception {
        Company company = createCompanyWithInvite();

        SignupRequest signup = new SignupRequest();
        signup.setName("Charlie");
        signup.setEmail("charlie.integration@test");
        signup.setPassword("password789");
        signup.setCompanyInviteCode(company.getInviteCode());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());

        AuthRequest login = new AuthRequest();
        login.setEmail(signup.getEmail());
        login.setPassword(signup.getPassword());

        var mvcResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        assertThat(body).contains("token");
    }

    @Test
    void me_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        Company company = createCompanyWithInvite();

        SignupRequest signup = new SignupRequest();
        signup.setName("Dana");
        signup.setEmail("dana.integration@test");
        signup.setPassword("password999");
        signup.setCompanyInviteCode(company.getInviteCode());

        var signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(signupResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dana.integration@test"))
                .andExpect(jsonPath("$.company.id").value(company.getId().toString()));
    }
}

