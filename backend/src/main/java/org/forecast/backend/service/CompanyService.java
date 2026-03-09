package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.company.CreateCompanyRequest;
import org.forecast.backend.dtos.company.UpdateCompanyRequest;
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
        company.setName(requireNonBlank(request.getName(), "Company name is required"));
        company.setLogoUrl(null);
        company.setAddress(requireNonBlank(request.getAddress(), "Address is required"));
        company.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
        company.setPhoneNumber(requireNonBlank(request.getPhoneNumber(), "Phone is required"));
        company.setWebsite(requireNonBlank(request.getWebsite(), "Website is required"));
        company.setIban(requireNonBlank(request.getIban(), "IBAN is required"));
        company.setVatNumber(requireNonBlank(request.getVatNumber(), "VAT number is required"));
        return companyRepository.save(company);
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
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

        if (request.getName() != null) company.setName(requireNonBlank(request.getName(), "Company name is required"));
        if (request.getLogoUrl() != null) company.setLogoUrl(requireNonBlank(request.getLogoUrl(), "Logo URL is required"));
        if (request.getAddress() != null) company.setAddress(requireNonBlank(request.getAddress(), "Address is required"));
        if (request.getEmail() != null) company.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
        if (request.getPhone() != null) company.setPhoneNumber(requireNonBlank(request.getPhone(), "Phone is required"));
        if (request.getWebsite() != null) company.setWebsite(requireNonBlank(request.getWebsite(), "Website is required"));
        if (request.getIban() != null) company.setIban(requireNonBlank(request.getIban(), "IBAN is required"));
        if (request.getVatNumber() != null) company.setVatNumber(requireNonBlank(request.getVatNumber(), "VAT number is required"));

        return companyRepository.save(company);
    }

    public Company updateLogoUrl(UUID id, String logoUrl) {
        Company company = getById(id);
        company.setLogoUrl(logoUrl);
        return companyRepository.save(company);
    }
}
