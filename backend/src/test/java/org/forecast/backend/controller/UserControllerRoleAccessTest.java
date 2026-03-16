package org.forecast.backend.controller;

import org.forecast.backend.model.User;
import org.forecast.backend.service.JwtService;
import org.forecast.backend.service.UserService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeRole_cannotListUsers_forbidden() throws Exception {
        var result = mockMvc.perform(get("/api/v1/users").accept(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        System.out.println("DEBUG: response status=" + status + ", body='" + body + "'");

        assertThat(status).isEqualTo(403);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_canListUsers_ok() throws Exception {
        // return an empty page to satisfy controller
        when(userService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new User()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

