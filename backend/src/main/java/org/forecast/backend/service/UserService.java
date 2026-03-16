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
        UUID currentCompanyId = companySecurityService.getCurrentCompanyId();
        if (currentCompanyId != null) {
            return userRepository.findByCompanyId(currentCompanyId);
        }
        return userRepository.findAll();
    }

    public Page<User> listAll(Pageable pageable) {
        UUID currentCompanyId = companySecurityService.getCurrentCompanyId();
        if (currentCompanyId != null) {
            return userRepository.findByCompanyId(currentCompanyId, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public User getById(UUID id) {
        UUID currentCompanyId = companySecurityService.getCurrentCompanyId();
        if (currentCompanyId != null) {
            return userRepository.findByIdAndCompanyId(id, currentCompanyId)
                    .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id + " found."));
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id + " found."));
    }

    public User create(CreateUserRequest request) {
        Company company;
        UUID currentCompanyId = companySecurityService.getCurrentCompanyId();
        if (currentCompanyId != null) {
            if (request.getCompanyId() != null && !currentCompanyId.equals(request.getCompanyId())) {
                throw new AccessDeniedException("Users can only be created in the authenticated company");
            }
            company = companyRepository.findById(currentCompanyId)
                    .orElseThrow(() -> new ResourceNotFoundException("No company with id " + currentCompanyId + " found."));
        } else if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("No company with id " + request.getCompanyId() + " found."));
        } else {
            String code = requireNonBlank(request.getCompanyInviteCode(), "Company invite code is required");

            var now = Instant.now();
            var maybeValid = companyRepository.findByInviteCodeAndNotExpired(code, now);
            if (maybeValid.isPresent()) {
                company = maybeValid.get();
            } else {
                var maybeAny = companyRepository.findByInviteCode(code);
                if (maybeAny.isPresent()) {
                    var found = maybeAny.get();
                    if (found.getInviteCodeExpiresAt() != null && !found.getInviteCodeExpiresAt().isAfter(now)) {
                        throw new InviteExpiredException("Company invite code has expired");
                    }
                }

                throw new ResourceNotFoundException("Invalid or expired company invite code");
            }
        }

        String normalizedEmail = requireNonBlank(request.getEmail(), "Email is required").trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(requireNonBlank(request.getName(), "User name is required"));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(requireNonBlank(request.getPassword(), "Password is required")));

        // Self-signup should never be able to escalate roles.
        user.setRole(Role.VIEWER);

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
        if (role == null) throw new IllegalArgumentException("Role is required");

        User user = getById(id);

        Role currentRole = user.getRole();
        if (currentRole == null) {
            throw new IllegalStateException("Current user role is missing");
        }

        // Keep same-role updates idempotent.
        if (currentRole == role) {
            return user;
        }

        boolean isAllowedUpgrade =
                (currentRole == Role.VIEWER && role == Role.FINANCE)
                        || (currentRole == Role.FINANCE && role == Role.ADMIN);

        if (!isAllowedUpgrade) {
            throw new IllegalArgumentException(
                    "Invalid role transition: " + currentRole + " -> " + role +
                            ". Allowed upgrades are VIEWER -> FINANCE and FINANCE -> ADMIN.");
        }

        user.setRole(role);

        return userRepository.save(user);
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
}
