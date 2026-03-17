package org.forecast.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRequest;
import org.forecast.backend.enums.Role;
import org.forecast.backend.exceptions.InviteExpiredException;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.exceptions.UserNotFoundException;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.forecast.backend.repository.CompanyRepository;
import org.forecast.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;

    private final PasswordEncoder passwordEncoder;
    private final CompanySecurityService companySecurityService;

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public List<User> listAll() {
        if (companySecurityService.isSystemAdmin()) {
            return userRepository.findAll();
        }
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Access denied.");
        return userRepository.findByCompanyId(currentCompanyId);
    }

    public Page<User> listAll(Pageable pageable) {
        if (companySecurityService.isSystemAdmin()) {
            return userRepository.findAll(pageable);
        }
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Access denied.");
        return userRepository.findByCompanyId(currentCompanyId, pageable);
    }

    public User getById(UUID id) {
        if (companySecurityService.isSystemAdmin()) {
            return userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id + " found."));
        }

        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Access denied.");
        return userRepository.findByIdAndCompanyId(id, currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id + " found."));
    }

    public User create(CreateUserRequest request) {
        Role currentRole = companySecurityService.getCurrentRole();
        Company company = resolveCompanyForCreate(request, currentRole);

        String normalizedEmail = requireNonBlank(request.getEmail(), "Email is required").trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(requireNonBlank(request.getName(), "User name is required"));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(requireNonBlank(request.getPassword(), "Password is required")));
        user.setRole(resolveRoleForCreate(request, currentRole));
        user.setCompany(company);
        user.setProfilePictureUrl(request.getProfilePictureUrl());

        return userRepository.save(user);
    }

    public User update(UUID id, UpdateUserRequest request) {
        User user = getById(id);

        if (request.getName() != null) user.setName(requireNonBlank(request.getName(), "User name is required"));
        if (request.getEmail() != null) user.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
        if (request.getProfilePictureUrl() != null)
            user.setProfilePictureUrl(requireNonBlank(request.getProfilePictureUrl(), "Profile picture URL is required"));

        return userRepository.save(user);
    }

    public User updateRole(UUID id, Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        Role actorRole = companySecurityService.getCurrentRole();
        if (actorRole == Role.SYSTEM_ADMIN) {
            return updateRoleAsSystemAdmin(id, role);
        }
        if (actorRole == Role.COMPANY_ADMIN) {
            return promoteFinanceUserToCompanyAdmin(id, role);
        }
        throw new AccessDeniedException("Only system admins or company admins can update user roles.");
    }

    public User updateProfilePictureUrl(UUID id, String profilePictureUrl) {
        User user = getById(id);
        user.setProfilePictureUrl(profilePictureUrl);
        return userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("No user associated with" +
                " that email address"));
    }

    private Company resolveCompanyForCreate(CreateUserRequest request, Role currentRole) {
        if (currentRole == null) {
            return resolveCompanyFromInviteCode(request.getCompanyInviteCode());
        }

        if (currentRole == Role.SYSTEM_ADMIN) {
            if (request.getCompanyId() != null) {
                return companyRepository.findById(request.getCompanyId())
                        .orElseThrow(() -> new ResourceNotFoundException("No company with id " + request.getCompanyId() + " found."));
            }
            if (request.getCompanyInviteCode() != null && !request.getCompanyInviteCode().isBlank()) {
                return resolveCompanyFromInviteCode(request.getCompanyInviteCode());
            }
            throw new IllegalArgumentException("Company id or invite code is required");
        }

        if (currentRole != Role.COMPANY_ADMIN) {
            throw new AccessDeniedException("Only company admins or system admins can create users.");
        }

        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Access denied.");
        if (request.getCompanyId() != null && !currentCompanyId.equals(request.getCompanyId())) {
            throw new AccessDeniedException("Users can only be created in the authenticated company.");
        }
        if (request.getCompanyInviteCode() != null && !request.getCompanyInviteCode().isBlank()) {
            throw new IllegalArgumentException("Company admins cannot use invite codes when creating users.");
        }
        return companyRepository.findById(currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + currentCompanyId + " found."));
    }

    private Company resolveCompanyFromInviteCode(String inviteCode) {
        String code = requireNonBlank(inviteCode, "Company invite code is required");

        Instant now = Instant.now();
        var maybeValid = companyRepository.findByInviteCodeAndNotExpired(code, now);
        if (maybeValid.isPresent()) {
            return maybeValid.get();
        }

        var maybeAny = companyRepository.findByInviteCode(code);
        if (maybeAny.isPresent()) {
            Company found = maybeAny.get();
            if (found.getInviteCodeExpiresAt() != null && !found.getInviteCodeExpiresAt().isAfter(now)) {
                throw new InviteExpiredException("Company invite code has expired");
            }
        }

        throw new ResourceNotFoundException("Invalid or expired company invite code");
    }

    private Role resolveRoleForCreate(CreateUserRequest request, Role currentRole) {
        if (currentRole == null) {
            return Role.FINANCE;
        }

        Role requestedRole = request.getRole();
        if (currentRole == Role.SYSTEM_ADMIN) {
            if (requestedRole == null) {
                return Role.FINANCE;
            }
            if (requestedRole == Role.SYSTEM_ADMIN) {
                throw new IllegalArgumentException("SYSTEM_ADMIN cannot be assigned.");
            }
            return requestedRole;
        }

        if (currentRole == Role.COMPANY_ADMIN) {
            if (requestedRole == null || requestedRole == Role.FINANCE) {
                return Role.FINANCE;
            }
            throw new AccessDeniedException("Company admins can only create FINANCE users.");
        }

        throw new AccessDeniedException("Only company admins or system admins can create users.");
    }

    private User updateRoleAsSystemAdmin(UUID id, Role role) {
        if (role == Role.SYSTEM_ADMIN) {
            throw new IllegalArgumentException("SYSTEM_ADMIN cannot be assigned through this endpoint.");
        }

        User user = getById(id);
        if (user.getRole() == Role.SYSTEM_ADMIN) {
            throw new IllegalArgumentException("SYSTEM_ADMIN users cannot be modified through this endpoint.");
        }
        if (user.getRole() == role) {
            return user;
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    private User promoteFinanceUserToCompanyAdmin(UUID id, Role role) {
        if (role != Role.COMPANY_ADMIN) {
            throw new AccessDeniedException("Company admins can only promote FINANCE users to COMPANY_ADMIN.");
        }

        User user = getById(id);
        if (user.getRole() == Role.COMPANY_ADMIN) {
            return user;
        }
        if (user.getRole() != Role.FINANCE) {
            throw new IllegalArgumentException("Only FINANCE users can be promoted to COMPANY_ADMIN.");
        }

        user.setRole(Role.COMPANY_ADMIN);
        return userRepository.save(user);
    }
}
