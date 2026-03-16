package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.forecast.backend.dtos.company.CreateCompanyRequest;
import org.forecast.backend.dtos.company.UpdateCompanyRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Company;
import org.forecast.backend.service.CompanyLogoStorageService;
import org.forecast.backend.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CompanyController.class)
@WebMvcTestWithTestSecurity
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    CompanyLogoStorageService companyLogoStorageService;

    private static Company company(UUID id) {
        Company c = new Company();
        c.setId(id);
        c.setName("Acme Cable");
        c.setEmail("info@acme.test");
        c.setPhoneNumber("+1 555 0100");
        c.setWebsite("https://acme.test");
        c.setIban("DE89370400440532013000");
        c.setVatNumber("DE123456789");
        return c;
    }

    @Test
    void listCompanies_returnsList() throws Exception {
        when(companyService.listAll()).thenReturn(List.of(company(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Acme Cable"));

        verify(companyService).listAll();
    }

    @Test
    void createCompany_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        CreateCompanyRequest req = CreateCompanyRequest.builder()
                .name("Acme Cable")
                .address("123 Main St, Berlin")
                .email("info@acme.test")
                .phoneNumber("+49 30 1234567")
                .website("https://acme.test")
                .iban("DE89370400440532013000")
                .vatNumber("DE123456789")
                .build();

        Company created = company(id);
        created.setLogoUrl(null);
        created.setAddress(req.getAddress());
        created.setPhoneNumber(req.getPhoneNumber());
        created.setIban(req.getIban());
        created.setVatNumber(req.getVatNumber());

        when(companyService.create(any(CreateCompanyRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("info@acme.test"))
                .andExpect(jsonPath("$.phoneNumber").value("+49 30 1234567"));

        verify(companyService).create(any(CreateCompanyRequest.class));
    }

    @Test
    void getCompany_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(companyService.getById(id)).thenThrow(new ResourceNotFoundException("No company"));

        mockMvc.perform(get("/api/v1/companies/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(companyService).getById(id);
    }

    @Test
    void updateCompany_validationError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCompanyRequest req = UpdateCompanyRequest.builder()
                .email("not-an-email")
                .phone("bad#phone")
                .build();

        mockMvc.perform(patch("/api/v1/companies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(companyService);
    }

    @Test
    void updateCompany_happyPath_returnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCompanyRequest req = UpdateCompanyRequest.builder()
                .email("support@acme.test")
                .build();

        Company updated = company(id);
        updated.setEmail("support@acme.test");

        when(companyService.update(eq(id), any(UpdateCompanyRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/companies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("support@acme.test"));

        verify(companyService).update(eq(id), any(UpdateCompanyRequest.class));
    }

    @Test
    void uploadCompanyLogo_happyPath_returnsUpdatedCompany() throws Exception {
        UUID id = UUID.randomUUID();

        Company updated = company(id);
        updated.setLogoUrl("/uploads/company-logos/" + id + ".png");

        when(companyLogoStorageService.storeCompanyLogo(eq(id), any(MultipartFile.class)))
                .thenReturn(updated.getLogoUrl());
        when(companyService.updateLogoUrl(eq(id), eq(updated.getLogoUrl()))).thenReturn(updated);

        mockMvc.perform(multipart("/api/v1/companies/{id}/logo", id)
                        .file("file", "fake".getBytes())
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.logoUrl").value(updated.getLogoUrl()));

        verify(companyLogoStorageService).storeCompanyLogo(eq(id), any(MultipartFile.class));
        verify(companyService).updateLogoUrl(eq(id), eq(updated.getLogoUrl()));
    }

    @Test
    void uploadCompanyLogo_storageRejectsFile_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        when(companyLogoStorageService.storeCompanyLogo(eq(id), any(MultipartFile.class)))
                .thenThrow(new IllegalArgumentException("Unsupported logo content type"));

        mockMvc.perform(multipart("/api/v1/companies/{id}/logo", id)
                        .file("file", "fake".getBytes())
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"));

        verify(companyLogoStorageService).storeCompanyLogo(eq(id), any(MultipartFile.class));
        verify(companyService, never()).updateLogoUrl(any(), any());
    }
}
