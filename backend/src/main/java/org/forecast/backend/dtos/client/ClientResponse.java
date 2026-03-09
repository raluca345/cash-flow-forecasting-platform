package org.forecast.backend.dtos.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.dtos.shared.PaginatedResponse;
import org.forecast.backend.model.Client;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {

    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private String vatNumber;
    private String address;

    public static ClientResponse fromEntity(Client client) {
        if (client == null) return null;
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .vatNumber(client.getVatNumber())
                .address(client.getAddress())
                .build();
    }

    public static List<ClientResponse> fromEntities(List<Client> clients) {
        return clients == null ? List.of() : clients.stream().map(ClientResponse::fromEntity).toList();
    }

    public static PaginatedResponse<ClientResponse> toPaginatedResponse(Page<Client> page) {
        return PaginatedResponse.fromPageContent(page, fromEntities(page.getContent()));
    }
}
