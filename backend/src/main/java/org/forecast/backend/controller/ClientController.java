package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.client.ClientResponse;
import org.forecast.backend.dtos.client.CreateClientRequest;
import org.forecast.backend.dtos.client.UpdateClientRequest;
import org.forecast.backend.dtos.shared.PaginatedResponse;
import org.forecast.backend.model.Client;
import org.forecast.backend.service.ClientService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<PaginatedResponse<ClientResponse>> listClients(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ClientResponse.toPaginatedResponse(clientService.listAll(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        Client created = clientService.create(request);
        return ResponseEntity.ok(ClientResponse.fromEntity(created));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID clientId) {
        Client client = clientService.get(clientId);
        return ResponseEntity.ok(ClientResponse.fromEntity(client));
    }

    @PatchMapping("/{clientId}")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateClientRequest request
    ) {
        Client updated = clientService.update(clientId, request);
        return ResponseEntity.ok(ClientResponse.fromEntity(updated));
    }
}
