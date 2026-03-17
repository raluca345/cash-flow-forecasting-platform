package org.forecast.backend.service;

import java.util.Optional;
import java.util.UUID;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void create_selfSignupEncodesPasswordAndDefaultsRoleToFinance() {
        Company company = new Company();
        company.setId(UUID.randomUUID());

        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .role(Role.COMPANY_ADMIN)
                .companyInviteCode("INVITE123")
                .profilePictureUrl("/uploads/tmp/jane.png")
                .build();

        when(companyRepository.findByInviteCodeAndNotExpired(any(), any())).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("jane@acme.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals("Jane Doe", created.getName());
        assertEquals("jane@acme.test", created.getEmail());
        assertEquals("encoded-secret", created.getPassword());
        assertEquals(Role.FINANCE, created.getRole());
        assertEquals(company, created.getCompany());
        assertEquals("/uploads/tmp/jane.png", created.getProfilePictureUrl());

        assertEquals("encoded-secret", savedUser.getPassword());
        assertNotEquals("secret123", savedUser.getPassword());
        assertNotNull(savedUser.getPassword());
        assertTrue(savedUser.getPassword().startsWith("encoded"));
    }

    @Test
    void create_whenSystemAdminCanCreateCompanyAdmin() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);

        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .role(Role.COMPANY_ADMIN)
                .companyId(companyId)
                .build();

        when(companySecurityService.getCurrentRole()).thenReturn(Role.SYSTEM_ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("jane@acme.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(request);

        assertEquals(Role.COMPANY_ADMIN, created.getRole());
        assertEquals(company, created.getCompany());
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

        when(companySecurityService.getCurrentRole()).thenReturn(Role.COMPANY_ADMIN);
        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(authenticatedCompanyId);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.create(request)
        );

        verify(companyRepository, never()).findById(requestedCompanyId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_whenCompanyAdminRequestsCompanyAdmin_rejects() {
        UUID companyId = UUID.randomUUID();

        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane Doe")
                .email("jane@acme.test")
                .password("secret123")
                .role(Role.COMPANY_ADMIN)
                .companyId(companyId)
                .build();

        when(companySecurityService.getCurrentRole()).thenReturn(Role.COMPANY_ADMIN);
        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company()));
        when(userRepository.findByEmail("jane@acme.test")).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> userService.create(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_requiresSystemAdmin() {
        UUID userId = UUID.randomUUID();
        when(companySecurityService.getCurrentRole()).thenReturn(Role.FINANCE);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.updateRole(userId, Role.FINANCE)
        );

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_systemAdminCanSetCompanyAdmin() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.FINANCE);

        when(companySecurityService.getCurrentRole()).thenReturn(Role.SYSTEM_ADMIN);
        when(companySecurityService.isSystemAdmin()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(userId, Role.COMPANY_ADMIN);

        assertEquals(Role.COMPANY_ADMIN, updated.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void updateRole_rejectsAssigningSystemAdmin() {
        UUID userId = UUID.randomUUID();
        when(companySecurityService.getCurrentRole()).thenReturn(Role.SYSTEM_ADMIN);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateRole(userId, Role.SYSTEM_ADMIN)
        );

        assertTrue(ex.getMessage().contains("SYSTEM_ADMIN"));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_whenTargetEqualsCurrent_doesNotSave() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.FINANCE);

        when(companySecurityService.getCurrentRole()).thenReturn(Role.SYSTEM_ADMIN);
        when(companySecurityService.isSystemAdmin()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        User updated = userService.updateRole(userId, Role.FINANCE);

        assertSame(existing, updated);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRole_companyAdminCanPromoteFinanceUserToCompanyAdmin() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setRole(Role.FINANCE);

        when(companySecurityService.getCurrentRole()).thenReturn(Role.COMPANY_ADMIN);
        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(companyId);
        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(userId, Role.COMPANY_ADMIN);

        assertEquals(Role.COMPANY_ADMIN, updated.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void updateRole_companyAdminCannotAssignFinance() {
        UUID userId = UUID.randomUUID();
        when(companySecurityService.getCurrentRole()).thenReturn(Role.COMPANY_ADMIN);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> userService.updateRole(userId, Role.FINANCE)
        );

        assertTrue(ex.getMessage().contains("promote FINANCE users"));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
