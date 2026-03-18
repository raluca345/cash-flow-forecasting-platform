package org.forecast.backend.service;

import java.util.Optional;
import java.util.UUID;
import org.forecast.backend.dtos.client.UpdateClientRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.repository.ClientRepository;
import org.forecast.backend.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySecurityService companySecurityService;

    @InjectMocks
    private ClientService clientService;

    @Test
    void get_whenClientBelongsToAnotherCompany_throwsResourceNotFound() {
        UUID clientId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(companyId);
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.get(clientId)
        );

        assertEquals("Client not found", ex.getMessage());
        verify(clientRepository).findByIdAndCompanyId(clientId, companyId);
    }

    @Test
    void update_whenClientBelongsToAnotherCompany_throwsResourceNotFound() {
        UUID clientId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        UpdateClientRequest request = UpdateClientRequest.builder()
                .address("New address")
                .build();

        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(companyId);
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.update(clientId, request)
        );

        assertEquals("Client not found", ex.getMessage());
        verify(clientRepository).findByIdAndCompanyId(clientId, companyId);
        verify(clientRepository, never()).save(any(Client.class));
    }
}
