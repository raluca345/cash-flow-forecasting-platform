package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.CreateCompanyRequest;
import org.forecast.backend.dtos.CompanyResponse;
import org.forecast.backend.dtos.UpdateCompanyRequest;
import org.forecast.backend.model.Company;
import org.forecast.backend.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<Company> companies = companyService.listAll();
        return ResponseEntity.ok(companies.stream().map(CompanyResponse::fromEntity).toList());
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        Company created = companyService.create(request);
        return ResponseEntity.ok(CompanyResponse.fromEntity(created));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID companyId) {
        Company company = companyService.getById(companyId);
        return ResponseEntity.ok(CompanyResponse.fromEntity(company));
    }

    @PatchMapping("/{companyId}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        Company updated = companyService.update(companyId, request);
        return ResponseEntity.ok(CompanyResponse.fromEntity(updated));
    }
}
