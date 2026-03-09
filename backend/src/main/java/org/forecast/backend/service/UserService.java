package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRequest;
import org.forecast.backend.enums.Role;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.forecast.backend.repository.CompanyRepository;
import org.forecast.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;

    public List<User> listAll() {
        return userRepository.findAll();
    }

    public Page<User> listAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id + " found."));
    }

    public User create(CreateUserRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + request.getCompanyId() + " found."));

        User user = new User();
        user.setName(requireNonBlank(request.getName(), "User name is required"));
        user.setEmail(requireNonBlank(request.getEmail(), "Email is required"));

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

    /**
     * Dedicated role update method. In a real system this must be protected so only admins can call it.
     */
    public User updateRole(UUID id, Role role) {
        if (role == null) throw new IllegalArgumentException("Role is required");

        User user = getById(id);

        // For now, allow setting any role. Later: enforce upgrade rules (e.g. VIEWER -> FINANCE only)
        // and company-admin authorization.
        user.setRole(role);

        return userRepository.save(user);
    }

    public User updateProfilePictureUrl(UUID id, String profilePictureUrl) {
        User user = getById(id);
        user.setProfilePictureUrl(profilePictureUrl);
        return userRepository.save(user);
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
