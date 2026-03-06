package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.CreateCompanyRequest;
import org.forecast.backend.dtos.UpdateCompanyRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Company;
import org.forecast.backend.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company create(CreateCompanyRequest request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setLogoUrl(request.getLogoUrl());
        company.setAddress(request.getAddress());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setWebsite(request.getWebsite());
        company.setIban(request.getIban());
        company.setVatNumber(request.getVatNumber());
        return companyRepository.save(company);
    }

    public List<Company> listAll() {
        return companyRepository.findAll();
    }

    public Company getById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + id + " found."));
    }

    public Company update(UUID id, UpdateCompanyRequest request) {
        Company company = getById(id);

        if (request.getName() != null) company.setName(request.getName());
        if (request.getLogoUrl() != null) company.setLogoUrl(request.getLogoUrl());
        if (request.getAddress() != null) company.setAddress(request.getAddress());
        if (request.getEmail() != null) company.setEmail(request.getEmail());
        if (request.getPhone() != null) company.setPhone(request.getPhone());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getIban() != null) company.setIban(request.getIban());
        if (request.getVatNumber() != null) company.setVatNumber(request.getVatNumber());

        return companyRepository.save(company);
    }
}
