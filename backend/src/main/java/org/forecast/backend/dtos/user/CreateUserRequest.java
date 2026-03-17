package org.forecast.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    // Role is optional for self-signup flows. Service defaults self-signup to FINANCE.
    private Role role;

    // Either companyId or companyInviteCode may be provided. Self-signup requires an invite code.
    private UUID companyId;

    @Size(max = 64, message = "Company invite code must be at most 64 characters")
    private String companyInviteCode;

    @Size(max = 2048, message = "Profile picture URL must be at most 2048 characters")
    private String profilePictureUrl;
}
