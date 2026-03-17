package org.forecast.backend.controller;

import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.forecast.backend.model.Company;
import org.forecast.backend.service.CompanyLogoStorageService;
import org.forecast.backend.service.CompanyService;
import org.forecast.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = CompanyController.class)
@WebMvcTestWithTestSecurity
class CompanyInviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CompanyLogoStorageService companyLogoStorageService;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void generateInviteCode_returnsCode_forCompanyAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(companyService.generateInviteCode(id)).thenReturn("INV12345");
        Company c = new Company();
        c.setId(id);
        c.setInviteCode("INV12345");
        c.setInviteCodeExpiresAt(java.time.Instant.now().plusSeconds(3600));
        when(companyService.getById(id)).thenReturn(c);

        mockMvc.perform(post("/api/v1/companies/" + id + "/invite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.inviteCode").value("INV12345"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiresAt").exists());
    }
}
