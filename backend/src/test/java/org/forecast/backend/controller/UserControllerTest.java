package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRoleRequest;
import org.forecast.backend.enums.Role;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.exceptions.InviteExpiredException;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.forecast.backend.service.JwtService;
import org.forecast.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@WebMvcTestWithTestSecurity
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static User user(UUID id, UUID companyId) {
        Company c = new Company();
        c.setId(companyId);

        User u = new User();
        u.setId(id);
        u.setName("Jane Doe");
        u.setEmail("jane@acme.test");
        u.setRole(Role.FINANCE);
        u.setCompany(c);
        u.setProfilePictureUrl(null);
        return u;
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void listUsers_returnsList() throws Exception {
        User u = user(UUID.randomUUID(), UUID.randomUUID());
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1);
        when(userService.listAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Jane Doe"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).listAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createUser_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        CreateUserRequest req = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .role(Role.COMPANY_ADMIN)
                .companyId(companyId)
                .profilePictureUrl("/uploads/tmp/abc.png")
                .build();

        User created = user(userId, companyId);
        created.setRole(Role.COMPANY_ADMIN);
        created.setProfilePictureUrl(req.getProfilePictureUrl());

        when(userService.create(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.role").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.profilePictureUrl").value(req.getProfilePictureUrl()));

        verify(userService).create(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getUser_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getById(userId)).thenThrow(new ResourceNotFoundException("No user"));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(userService).getById(userId);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void updateUser_validationError_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void updateUser_happyPath_returnsUpdated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        UpdateUserRequest req = UpdateUserRequest.builder()
                .profilePictureUrl("/uploads/user-pics/" + userId + ".png")
                .build();

        User updated = user(userId, companyId);
        updated.setProfilePictureUrl(req.getProfilePictureUrl());

        when(userService.update(eq(userId), any(UpdateUserRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePictureUrl").value(req.getProfilePictureUrl()));

        verify(userService).update(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void setProfilePictureUrl_happyPath_returnsUpdated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        String url = "/uploads/tmp/" + userId + ".svg";

        User updated = user(userId, companyId);
        updated.setProfilePictureUrl(url);

        when(userService.updateProfilePictureUrl(eq(userId), eq(url))).thenReturn(updated);

        mockMvc.perform(post("/api/v1/users/{id}/profile-picture", userId)
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.profilePictureUrl").value(url));

        verify(userService).updateProfilePictureUrl(eq(userId), eq(url));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void updateUserRole_happyPath_returnsUpdated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role(Role.FINANCE)
                .build();

        User updated = user(userId, companyId);
        updated.setRole(Role.FINANCE);

        when(userService.updateRole(eq(userId), eq(Role.FINANCE))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/users/{id}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("FINANCE"));

        verify(userService).updateRole(eq(userId), eq(Role.FINANCE));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void updateUserRole_missingRole_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder().build();

        mockMvc.perform(patch("/api/v1/users/{id}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void updateUserRole_forbiddenTransition_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role(Role.COMPANY_ADMIN)
                .build();

        when(userService.updateRole(eq(userId), eq(Role.COMPANY_ADMIN)))
                .thenThrow(new IllegalArgumentException("Invalid role transition"));

        mockMvc.perform(patch("/api/v1/users/{id}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Invalid role transition"));

        verify(userService).updateRole(eq(userId), eq(Role.COMPANY_ADMIN));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createUser_withExpiredInvite_returns410() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .name("John Doe")
                .email("john@expired.test")
                .password("password123")
                .companyInviteCode("EXPIRED123")
                .build();

        when(userService.create(any(CreateUserRequest.class))).thenThrow(new InviteExpiredException("Company invite code has expired"));

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("INVITE_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Company invite code has expired"));

        verify(userService).create(any(CreateUserRequest.class));
    }
}
