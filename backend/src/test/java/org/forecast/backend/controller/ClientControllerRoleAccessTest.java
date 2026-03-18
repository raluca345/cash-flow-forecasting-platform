package org.forecast.backend.controller;

import java.util.List;
import java.util.UUID;
import org.forecast.backend.model.Client;
import org.forecast.backend.service.ClientService;
import org.forecast.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClientControllerRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static Client client() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setName("Acme Client");
        return client;
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdmin_cannotListClients_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(clientService, never()).listAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void finance_canListClients_ok() throws Exception {
        when(clientService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(client()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void companyAdmin_canListClients_viaRoleHierarchy_ok() throws Exception {
        when(clientService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(client()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
