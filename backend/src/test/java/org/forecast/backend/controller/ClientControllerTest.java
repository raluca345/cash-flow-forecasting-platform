package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.forecast.backend.dtos.client.CreateClientRequest;
import org.forecast.backend.dtos.client.UpdateClientRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
@WebMvcTestWithTestSecurity
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    private static Client client(UUID id) {
        Client c = new Client();
        c.setId(id);
        c.setName("Berlin Bikes GmbH");
        c.setEmail("billing@berlin-bikes.test");
        c.setPhoneNumber("+49 30 1234567");
        c.setVatNumber("DE123456789");
        c.setAddress("Alexanderplatz 1, 10178 Berlin");
        return c;
    }

    @Test
    void listClients_returnsList() throws Exception {
        Client c = client(UUID.randomUUID());
        Page<Client> page = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);
        when(clientService.listAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Berlin Bikes GmbH"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(clientService).listAll(any(Pageable.class));
    }

    @Test
    void createClient_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        CreateClientRequest req = CreateClientRequest.builder()
                .name("Berlin Bikes GmbH")
                .email("billing@berlin-bikes.test")
                .phoneNumber("+49 30 1234567")
                .vatNumber(null)
                .address("Alexanderplatz 1, 10178 Berlin")
                .build();

        Client created = client(id);
        created.setVatNumber(null);

        when(clientService.create(any(CreateClientRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.phoneNumber").value("+49 30 1234567"))
                .andExpect(jsonPath("$.vatNumber").doesNotExist());

        verify(clientService).create(any(CreateClientRequest.class));
    }

    @Test
    void createClient_missingAddress_returns400() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .name("Berlin Bikes GmbH")
                .email("billing@berlin-bikes.test")
                .phoneNumber("+49 30 1234567")
                .address(null)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.address").value("Address is required"));

        verifyNoInteractions(clientService);
    }

    @Test
    void getClient_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.get(id)).thenThrow(new ResourceNotFoundException("Client not found"));

        mockMvc.perform(get("/api/v1/clients/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(clientService).get(id);
    }

    @Test
    void updateClient_validationError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateClientRequest req = UpdateClientRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(patch("/api/v1/clients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(clientService);
    }

    @Test
    void updateClient_happyPath_returnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateClientRequest req = UpdateClientRequest.builder()
                .address("New Address 42")
                .build();

        Client updated = client(id);
        updated.setAddress("New Address 42");

        when(clientService.update(eq(id), any(UpdateClientRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/clients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("New Address 42"));

        verify(clientService).update(eq(id), any(UpdateClientRequest.class));
    }
}
