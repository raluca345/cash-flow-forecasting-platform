package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.CreateClientRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService implements  IClientService {

    private final ClientRepository clientRepository;

    @Override
    public Client create(CreateClientRequest request) {
        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        return clientRepository.save(client);
    }

    @Override
    public Client get(UUID clientId) {
        return clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }
}
