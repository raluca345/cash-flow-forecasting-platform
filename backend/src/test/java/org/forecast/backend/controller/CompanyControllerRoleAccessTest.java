package org.forecast.backend.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.forecast.backend.dtos.company.CreateCompanyRequest;
import org.forecast.backend.dtos.company.UpdateCompanyRequest;
import org.forecast.backend.model.Company;
import org.forecast.backend.service.CompanyLogoStorageService;
import org.forecast.backend.service.CompanyService;
import org.forecast.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyControllerRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyLogoStorageService companyLogoStorageService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static Company company(UUID id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme");
        company.setAddress("Main St");
        company.setEmail("info@acme.test");
        company.setPhoneNumber("+40 700 000 000");
        company.setWebsite("https://acme.test");
        company.setIban("DE89370400440532013000");
        company.setVatNumber("DE123456789");
        company.setInviteCode("INV12345");
        company.setInviteCodeExpiresAt(Instant.now().plusSeconds(3600));
        return company;
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeRole_cannotListCompanies_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/companies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(companyService, never()).listAll();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_canListCompanies_ok() throws Exception {
        when(companyService.listAll()).thenReturn(List.of(company(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/companies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_cannotCreateCompany_forbidden() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .name("New Co")
                .address("Main St")
                .email("billing@newco.test")
                .phoneNumber("+40 700 000 001")
                .website("https://newco.test")
                .iban("DE89370400440532013000")
                .vatNumber("DE123456780")
                .build();

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(companyService, never()).create(any(CreateCompanyRequest.class));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_canCreateCompany_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .name("New Co")
                .address("Main St")
                .email("billing@newco.test")
                .phoneNumber("+40 700 000 001")
                .website("https://newco.test")
                .iban("DE89370400440532013000")
                .vatNumber("DE123456780")
                .build();

        when(companyService.create(any(CreateCompanyRequest.class))).thenReturn(company(companyId));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()));
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeRole_cannotGetCompany_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/companies/{companyId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verify(companyService, never()).getById(any());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_canGetCompany_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.getById(companyId)).thenReturn(company(companyId));

        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_canGetCompany_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.getById(companyId)).thenReturn(company(companyId));

        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_cannotGenerateInviteCode_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/companies/{companyId}/invite", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(companyService, never()).generateInviteCode(any());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_canGenerateInviteCode_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId);
        when(companyService.generateInviteCode(companyId)).thenReturn("INV12345");
        when(companyService.getById(companyId)).thenReturn(company);

        mockMvc.perform(post("/api/v1/companies/{companyId}/invite", companyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value("INV12345"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_cannotUpdateCompany_forbidden() throws Exception {
        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .email("support@acme.test")
                .build();

        mockMvc.perform(patch("/api/v1/companies/{companyId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(companyService, never()).update(any(), any(UpdateCompanyRequest.class));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_canUpdateCompany_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .email("support@acme.test")
                .build();
        Company updated = company(companyId);
        updated.setEmail("support@acme.test");

        when(companyService.update(eq(companyId), any(UpdateCompanyRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/companies/{companyId}", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("support@acme.test"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_cannotUploadLogo_forbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/companies/{companyId}/logo", UUID.randomUUID())
                        .file("file", "fake".getBytes())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(companyLogoStorageService, never()).storeCompanyLogo(any(), any());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_canUploadLogo_ok() throws Exception {
        UUID companyId = UUID.randomUUID();
        Company updated = company(companyId);
        updated.setLogoUrl("/uploads/company-logos/" + companyId + ".png");

        when(companyLogoStorageService.storeCompanyLogo(eq(companyId), any()))
                .thenReturn(updated.getLogoUrl());
        when(companyService.updateLogoUrl(companyId, updated.getLogoUrl())).thenReturn(updated);

        mockMvc.perform(multipart("/api/v1/companies/{companyId}/logo", companyId)
                        .file("file", "fake".getBytes())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(updated.getLogoUrl()));
    }
}
