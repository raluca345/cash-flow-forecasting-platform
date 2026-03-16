package org.forecast.backend.service;

import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.enums.Role;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.forecast.backend.repository.CompanyRepository;
import org.forecast.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CompanySecurityService companySecurityService;

    @InjectMocks
    private UserService userService;

    @Test
    void create_encodesPasswordAndDefaultsRoleToViewer() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);

        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .role(Role.ADMIN)
                .companyId(companyId)
                .profilePictureUrl("/uploads/tmp/jane.png")
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals("Jane Doe", created.getName());
        assertEquals("jane@acme.test", created.getEmail());
        assertEquals("encoded-secret", created.getPassword());
        assertEquals(Role.VIEWER, created.getRole());
        assertEquals(company, created.getCompany());
        assertEquals("/uploads/tmp/jane.png", created.getProfilePictureUrl());

        assertEquals("encoded-secret", savedUser.getPassword());
        assertNotEquals("secret123", savedUser.getPassword());
        assertNotNull(savedUser.getPassword());
        assertTrue(savedUser.getPassword().startsWith("encoded"));
    }

    @Test
    void updateRole_allowsViewerToFinance() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.VIEWER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(userId, Role.FINANCE);

        assertEquals(Role.FINANCE, updated.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void updateRole_allowsFinanceToAdmin() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.FINANCE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(userId, Role.ADMIN);

        assertEquals(Role.ADMIN, updated.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void updateRole_rejectsSkippedUpgradeViewerToAdmin() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.VIEWER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateRole(userId, Role.ADMIN)
        );

        assertTrue(ex.getMessage().contains("Invalid role transition"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_rejectsDowngrade() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateRole(userId, Role.FINANCE)
        );

        assertTrue(ex.getMessage().contains("Invalid role transition"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_whenTargetEqualsCurrent_doesNotSave() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.FINANCE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        User updated = userService.updateRole(userId, Role.FINANCE);

        assertSame(existing, updated);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_whenAuthenticatedCompanyDiffers_rejectsCrossCompanyRequest() {
        UUID authenticatedCompanyId = UUID.randomUUID();
        UUID requestedCompanyId = UUID.randomUUID();

        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .companyId(requestedCompanyId)
                .build();

        when(companySecurityService.getCurrentCompanyId()).thenReturn(authenticatedCompanyId);

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> userService.create(request)
        );

        verify(companyRepository, never()).findById(requestedCompanyId);
        verify(userRepository, never()).save(any(User.class));
    }
}

