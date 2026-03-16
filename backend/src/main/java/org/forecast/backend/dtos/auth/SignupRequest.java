package org.forecast.backend.dtos.auth;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SignupRequest {

    @NotBlank(message = "User name is required")
    @Size(min = 2, max = 255, message = "User name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    private String password;

    @NotBlank(message = "Company invite code is required")
    @Size(min = 6, max = 64, message = "Invite code must be between 6 and 64 characters")
    private String companyInviteCode;

}
