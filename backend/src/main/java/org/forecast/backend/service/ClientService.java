package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.client.CreateClientRequest;
import org.forecast.backend.dtos.client.UpdateClientRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Company;
import org.forecast.backend.repository.ClientRepository;
import org.forecast.backend.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService implements  IClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final CompanySecurityService companySecurityService;

    @Override
    public Client create(CreateClientRequest request) {
        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhoneNumber(request.getPhoneNumber());
        client.setVatNumber(request.getVatNumber());
        client.setAddress(request.getAddress());

        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        Company company = companyRepository.findById(currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + currentCompanyId + " found."));
        client.setCompany(company);

        return clientRepository.save(client);
    }

    @Override
    public Client get(UUID clientId) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        return clientRepository.findByIdAndCompanyId(clientId, currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }

    public List<Client> listAll() {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        return clientRepository.findByCompanyId(currentCompanyId);
    }

    public Page<Client> listAll(Pageable pageable) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        return clientRepository.findByCompanyId(currentCompanyId, pageable);
    }

    public Client update(UUID clientId, UpdateClientRequest request) {
        Client client = get(clientId);

        if (request.getName() != null) client.setName(requireNonBlank(request.getName(), "Client name is required"));
        if (request.getEmail() != null) client.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
        if (request.getPhoneNumber() != null) client.setPhoneNumber(requireNonBlank(request.getPhoneNumber(), "Phone number is required"));
        if (request.getVatNumber() != null) client.setVatNumber(requireNonBlank(request.getVatNumber(), "VAT number is required"));
        if (request.getAddress() != null) client.setAddress(requireNonBlank(request.getAddress(), "Address is required"));

        return clientRepository.save(client);
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
