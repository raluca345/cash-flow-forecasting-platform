package org.forecast.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.enums.Role;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "User name is required")
    @Size(min = 2, max = 255, message = "User name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 320, message = "Email must be at most 320 characters")
    private String email;

    // Role is optional for self-signup flows (e.g. Google OAuth). Defaults to VIEWER in the service.
    private Role role;

    @NotNull(message = "Company id is required")
    private UUID companyId;

    @Size(max = 2048, message = "Profile picture URL must be at most 2048 characters")
    private String profilePictureUrl;
}

