package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.company.CompanyResponse;
import org.forecast.backend.dtos.company.CreateCompanyRequest;
import org.forecast.backend.dtos.company.UpdateCompanyRequest;
import org.forecast.backend.model.Company;
import org.forecast.backend.service.CompanyLogoStorageService;
import org.forecast.backend.service.CompanyService;
import org.forecast.backend.dtos.company.InviteCodeResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    private final CompanyLogoStorageService companyLogoStorageService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<Company> companies = companyService.listAll();
        return ResponseEntity.ok(companies.stream().map(CompanyResponse::fromEntity).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        Company created = companyService.create(request);
        return ResponseEntity.ok(CompanyResponse.fromEntity(created));
    }

    @PostMapping("/{companyId}/invite")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<InviteCodeResponse> generateInviteCode(@PathVariable UUID companyId) {
        String code = companyService.generateInviteCode(companyId);
        var company = companyService.getById(companyId);
        return ResponseEntity.ok(new InviteCodeResponse(code, company.getInviteCodeExpiresAt()));
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID companyId) {
        Company company = companyService.getById(companyId);
        return ResponseEntity.ok(CompanyResponse.fromEntity(company));
    }

    @PatchMapping("/{companyId}")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        Company updated = companyService.update(companyId, request);
        return ResponseEntity.ok(CompanyResponse.fromEntity(updated));
    }

    @PostMapping(value = "/{companyId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CompanyResponse> uploadCompanyLogo(
            @PathVariable UUID companyId,
            @RequestPart("file") MultipartFile file
    ) {
        String logoUrl = companyLogoStorageService.storeCompanyLogo(companyId, file);
        Company updated = companyService.updateLogoUrl(companyId, logoUrl);
        return ResponseEntity.ok(CompanyResponse.fromEntity(updated));
    }
}
